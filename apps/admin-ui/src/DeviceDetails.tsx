// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {useEffect,useRef,useState} from 'react';
import {createPortal} from 'react-dom';

/** Render outside the scrolling table so details remain readable on the last row. */
export function DeviceDetails({deviceId,text}:{deviceId:string;text:string}){
  const button=useRef<HTMLButtonElement>(null);const timer=useRef<ReturnType<typeof setTimeout>|undefined>(undefined);
  const [position,setPosition]=useState<{top:number;left:number}>();
  const id=`device-details-${encodeURIComponent(deviceId)}`;
  const cancel=()=>clearTimeout(timer.current);
  useEffect(()=>()=>clearTimeout(timer.current),[]);
  function show(){cancel();const rect=button.current?.getBoundingClientRect();if(rect)setPosition({top:Math.max(16,Math.min(rect.bottom+8,window.innerHeight-310)),left:Math.max(16,Math.min(rect.left,window.innerWidth-432))});}
  function leave(){cancel();timer.current=setTimeout(()=>setPosition(undefined),150);}
  return <span className="device-details" onMouseEnter={show} onMouseLeave={leave}>
    <button ref={button} className="device-info" aria-label={`Details for ${deviceId}`} aria-describedby={position?id:undefined} onFocus={show} onBlur={()=>{cancel();setPosition(undefined);}} onClick={show} onKeyDown={event=>{if(event.key==='Escape'){cancel();setPosition(undefined);}}}>ⓘ</button>
    {position&&createPortal(<span className="device-tooltip" role="tooltip" id={id} style={position} onMouseEnter={cancel} onMouseLeave={leave}>{text}</span>,document.body)}
  </span>;
}
