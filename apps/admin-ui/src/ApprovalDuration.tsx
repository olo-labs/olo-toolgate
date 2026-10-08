// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
export type ApprovalDurationValue={unlimited:boolean;until:string};
export function localDateTime(time:number){const date=new Date(time);return new Date(time-date.getTimezoneOffset()*60000).toISOString().slice(0,16);}
export function defaultDuration():ApprovalDurationValue{return {unlimited:false,until:localDateTime(Date.now()+86400000)};}
export function validDuration(value:ApprovalDurationValue,now:number){const time=new Date(value.until).getTime();return value.unlimited||Number.isSafeInteger(time)&&time>now;}
export function durationBody(value:ApprovalDurationValue){return value.unlimited?{unlimitedConnection:true}:{connectionExpiresAtUnixMs:new Date(value.until).getTime()};}
export function ApprovalDuration({value,onChange,now,disabled=false}:{value:ApprovalDurationValue;onChange:(value:ApprovalDurationValue)=>void;now:number;disabled?:boolean}){
  return <><label htmlFor="approval-duration">Connection duration</label><select id="approval-duration" value={value.unlimited?'unlimited':'limited'} onChange={event=>onChange({...value,unlimited:event.target.value==='unlimited'})} disabled={disabled}>
    <option value="limited">Until a date and time</option><option value="unlimited">Unlimited time</option></select>
    {!value.unlimited&&<><label htmlFor="connection-until">Allow connection until</label><input id="connection-until" type="datetime-local" required value={value.until} min={localDateTime(now)} onChange={event=>onChange({...value,until:event.target.value})} disabled={disabled} aria-describedby="connection-until-help"/>
      <p id="connection-until-help">Your local time ({Intl.DateTimeFormat().resolvedOptions().timeZone}). Access ends automatically at this time. Default: 24 hours.</p></>}
    {value.unlimited&&<p>Access stays approved until you deapprove or disable this device.</p>}
    {!validDuration(value,now)&&<p role="alert">Choose a future date and time.</p>}</>;
}
