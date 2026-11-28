# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""User-bound platform tools through existing public Control APIs.

This client never executes a graph or model, promotes SERVICE authority, or
resubmits an uncertain effect. The ordinary OLO runtime executes every tool.
"""
import argparse
import json
import os
import re
import time
from pathlib import Path

from tools.admin.control import Control,Refused
from tools.admin.import_workflow import call

ID=re.compile('[A-Za-z0-9][A-Za-z0-9._:-]{0,127}')


class Unresolved(Refused):
    def __init__(self,invocation_id,state):
        self.invocation_id=invocation_id;self.state=state
        super().__init__('Invocation '+invocation_id+' is '+state+'; inspect its current outcome without repeating the effect')


class UserTools:
    def __init__(self,client,binding_id,device_id):
        if not ID.fullmatch(binding_id) or not ID.fullmatch(device_id):raise Refused('Invalid reviewed tool placement')
        self.client=client;self.binding_id=binding_id;self.device_id=device_id

    def begin(self,tool_id,action,arguments,request_key):
        if not ID.fullmatch(request_key):raise Refused('Use a stable explicit request key')
        body=dict(bindingId=self.binding_id,deviceId=self.device_id,
            request=dict(toolId=tool_id,action=action,arguments=arguments))
        status,response=self.client.request('POST','/api/control/v1/access/human/invocations',
            json.dumps(body,separators=(',',':'),allow_nan=False).encode(),'application/json',request_key)
        if status not in (200,201,202) or not isinstance(response,dict):raise Refused('Invalid tool admission response')
        invocation=response.get('invocation',{})
        identifier=invocation.get('id','')
        if not ID.fullmatch(identifier):raise Refused('Missing authoritative invocation ID')
        return dict(invocationId=identifier,state=invocation['state'],requestKey=request_key,request=body)

    def result(self,invocation_id):
        if not ID.fullmatch(invocation_id):raise Refused('Invalid invocation ID')
        return call(self.client,'/api/control/v1/access/human/invocations/'+invocation_id+'/result')

    def wait(self,receipt,timeout=60):
        identifier=receipt['invocationId']
        if receipt['state']=='PENDING_APPROVAL':return dict(invocationId=identifier,state='PENDING_APPROVAL')
        deadline=time.monotonic()+timeout
        while True:
            response=self.result(identifier)
            record=response.get('record',{});outcome=response.get('operationOutcome',{})
            if record.get('requestId')!=identifier:
                raise Refused('Tool response belongs to another invocation')
            state=outcome.get('state')
            if outcome.get('finalized') is True and state=='SUCCEEDED':
                result=response.get('result') or {}
                if (record.get('state')!='DONE' or result.get('requestId')!=identifier
                        or result.get('error') is not None or not isinstance(result.get('output'),dict)):
                    raise Refused('Finalized tool result is missing')
                return dict(invocationId=identifier,state=state,operationOutcome=outcome,output=result['output'])
            if state in ('OUTCOME_UNKNOWN','RECONCILIATION_REQUIRED','FAILED','PARTIAL'):
                raise Unresolved(identifier,state)
            if record.get('state') in ('FAILED','EXPIRED'):
                raise Unresolved(identifier,state or record['state'])
            if time.monotonic()>=deadline:raise Unresolved(identifier,'PENDING')
            time.sleep(.25)

    def knowledge(self,operation,root_id,request,request_key):
        if operation not in ('rag.query','memory.read','memory.append'):
            raise Refused('Unsupported starter knowledge operation')
        import uuid
        if str(uuid.UUID(root_id))!=root_id:raise Refused('Use a canonical resource UUID')
        root='kbId' if operation=='rag.query' else 'conversationId'
        receipt=self.begin('olo.'+operation.replace('.','/'),operation,
            {root:root_id,'request':json.dumps(request,separators=(',',':'),allow_nan=False)},request_key)
        result=self.wait(receipt)
        if result['state']=='PENDING_APPROVAL':return result
        output=result['output']
        if set(output)!={'json'} or not isinstance(output['json'],str):raise Refused('Invalid knowledge result envelope')
        data=json.loads(output['json'])
        if not isinstance(data,dict):raise Refused('Invalid knowledge result')
        return {**result,'data':data}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--origin',required=True);parser.add_argument('--ca-file',type=Path)
    parser.add_argument('--token-file',type=Path,required=True)
    parser.add_argument('--binding-id',required=True);parser.add_argument('--device-id',required=True)
    parser.add_argument('--request-key',required=True)
    parser.add_argument('--output',type=Path,required=True,help='Fresh local evidence file; never a public release asset')
    command=parser.add_subparsers(dest='command',required=True)
    query=command.add_parser('query');query.add_argument('--kb-id',required=True);query.add_argument('--query',required=True)
    memory=command.add_parser('memory');memory.add_argument('--conversation-id',required=True)
    action=command.add_parser('apply');action.add_argument('--operation',choices=['restart','provision'],required=True)
    action.add_argument('--subject',required=True);action.add_argument('--reference',required=True)
    inspect=command.add_parser('inspect');inspect.add_argument('--kind',choices=['service','joiner'],required=True)
    inspect.add_argument('--subject',required=True);inspect.add_argument('--reference',required=True)
    args=parser.parse_args()
    tools=UserTools(Control(args.origin,args.token_file,args.ca_file),args.binding_id,args.device_id)
    if args.command in ('query','memory'):
        import uuid
        root_id=args.kb_id if args.command=='query' else args.conversation_id
        if str(uuid.UUID(root_id))!=root_id:raise Refused('Use a canonical resource UUID')
        operation='rag.query' if args.command=='query' else 'memory.read'
        request=dict(query=args.query) if args.command=='query' else dict(budget=4000)
        arguments={'kbId' if args.command=='query' else 'conversationId':root_id,
            'request':json.dumps(request,separators=(',',':'),allow_nan=False)}
        tool='olo.'+operation.replace('.','/')
    else:
        operation='inspect' if args.command=='inspect' else args.operation
        arguments=dict(kind=args.kind,subject=args.subject,reference=args.reference) if operation=='inspect' else {
            'serviceId' if operation=='restart' else 'joinerId':args.subject,'reference':args.reference}
        tool='olo.sandbox/'+operation;operation='sandbox.'+operation
    # Reserve an exclusive private journal before admission, including for reads.
    # An ambiguous HTTP response retains the key; a rerun cannot erase custody.
    journal=dict(state='ADMISSION_UNRESOLVED',requestKey=args.request_key,toolId=tool,action=operation)
    def save():
        import uuid
        temporary=args.output.with_name(args.output.name+'.'+uuid.uuid4().hex+'.next')
        descriptor=os.open(temporary,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
        with os.fdopen(descriptor,'w',encoding='utf-8') as stream:
            stream.write(json.dumps(journal,indent=2)+'\n');stream.flush();os.fsync(stream.fileno())
        os.replace(temporary,args.output)
    try:
        descriptor=os.open(args.output,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
    except FileExistsError:
        raise Refused('Evidence output already exists; inspect its invocation and request key before another request') from None
    with os.fdopen(descriptor,'w',encoding='utf-8') as stream:
        stream.write(json.dumps(journal,indent=2)+'\n');stream.flush();os.fsync(stream.fileno())
    receipt=tools.begin(tool,operation,arguments,args.request_key)
    journal.update(state='ADMITTED',admission=receipt);save()
    value=tools.wait(receipt)
    journal.update(state=value['state'],result=value);save()
    print(json.dumps(dict(invocationId=value['invocationId'],state=value['state'])))


if __name__=='__main__':main()
