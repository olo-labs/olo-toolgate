// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type {ControlSnapshot} from '@olo-labs/toolgate-contracts';
import releases from '../../../config/initial/releases.json';
declare const __APP_VERSION__:string;
type Collections=Pick<ControlSnapshot,'teams'|'agentGroups'|'deviceGroups'|'toolGroups'|'roles'|'grants'|'policies'|'bindings'>;
const files=import.meta.glob('../../../config/initial/*/*.json',{eager:true,import:'default'}) as Record<string,unknown>;
const bundle=(releases.releases as Record<string,string>)[__APP_VERSION__];
const prefix='../../../config/initial/'+bundle+'/';
const manifest=files[prefix+'manifest.json'] as {files:readonly string[]}|undefined;
if(!manifest)throw new Error('Release initial configuration is missing');
export const initialConfiguration=Object.assign({},...manifest.files.map(name=>files[prefix+name])) as Collections;
