import React, { useEffect, useRef, useState } from 'react';
import { api } from './api';

export function Modal({ title, close, children, busy = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog ref={ref} aria-labelledby="modal-title" onCancel={e => { e.preventDefault(); if (!busy) close(); }}>
    <div className="modal-head"><h2 id="modal-title">{title}</h2><button aria-label="Close" disabled={busy} onClick={close}>×</button></div>
    {children}
  </dialog>;
}

export function Auth({ onLogin }) {
  const [register, setRegister] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true); setError('');
    try { onLogin(await api('/account/' + (register ? 'register' : 'login'), 'POST', data)); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  return <main className="auth"><div className="book-art" aria-hidden="true"><div className="tabs"><i>A</i><i>G</i><i>M</i><i>S</i></div><div className="cover"><span>✽</span><strong>Address<br/>Book</strong><div className="cover-line"/></div></div>
    <section className="auth-box"><h1>{register ? 'Create an account' : 'Welcome back'}</h1>
    <p>{register ? 'Start your personal address book.' : 'Sign in to your address book.'}</p>
    <form onSubmit={submit} key={String(register)}>
      {register && <label>Your name<input name="name" autoComplete="name" required maxLength={100}/></label>}
      <label>Email or phone number<input name="identifier" autoComplete="username" required maxLength={254}/></label>
      <label>Password<input name="password" type="password" required minLength={register ? 10 : 1} maxLength={128} autoComplete={register ? 'new-password' : 'current-password'}/></label>
      {register && <small>Use at least 10 characters.</small>}
      {error && <p role="alert">{error}</p>}
      <button className="primary" disabled={busy}>{register ? 'Create account' : 'Sign in'}</button>
    </form><button className="text-button" disabled={busy} onClick={() => { setRegister(!register); setError(''); }}>{register ? 'Already registered? Sign in' : 'Create an account'}</button></section></main>;
}

function Channels({ kind, entries, change }) {
  const isEmail = kind === 'Email';
  function update(index, key, value) { change(entries.map((e, i) => i === index ? { ...e, [key]: value } : e)); }
  return <fieldset><legend>{isEmail ? 'Email addresses' : 'Phone numbers'}</legend>
    {entries.map((entry, index) => <div className="entry-row" key={entry.key}>
      <label>Label<input value={entry.label} required maxLength={30} onChange={e => update(index,'label',e.target.value)}/></label>
      <label>{kind}<input type={isEmail ? 'email' : 'tel'} value={entry.value} required maxLength={isEmail ? 254 : 32} onChange={e => update(index,'value',e.target.value)}/></label>
      <button type="button" aria-label={'Remove ' + kind + ' ' + (index+1)} onClick={() => change(entries.filter((_,i) => i !== index))}>×</button>
    </div>)}
    <button type="button" disabled={entries.length >= 20} onClick={() => change([...entries,{key: crypto.randomUUID(), label:'Personal',value:''}])}>+ Add {kind.toLowerCase()}</button>
  </fieldset>;
}

export function ContactEditor({ contact, close, saved }) {
  const [draft, setDraft] = useState(() => {
    const source = contact || {firstName:'',lastName:'',title:'',emails:[],phones:[]};
    const keyed = values => values.map(e => ({...e,key:crypto.randomUUID()}));
    return {...source,emails:keyed(source.emails),phones:keyed(source.phones)};
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  function set(key, value) { setDraft(old => ({...old,[key]:value})); }
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('');
    const clean = entries => entries.map(({label,value}) => ({label:label.trim(),value:value.trim()}));
    const payload = {firstName:draft.firstName,lastName:draft.lastName,title:draft.title,emails:clean(draft.emails),phones:clean(draft.phones)};
    try {
      await api(contact ? '/contacts/' + contact.id : '/contacts', contact ? 'PUT' : 'POST', payload);
      saved();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  return <Modal title={contact ? 'Edit contact' : 'New contact'} close={close} busy={busy}><form onSubmit={submit}>
    <div className="pair">{[['firstName','First name'],['lastName','Last name']].map(([key,label]) => <label key={key}>{label}<input required maxLength={80} value={draft[key]} onChange={e=>set(key,e.target.value)}/></label>)}</div>
    <label>Title<input maxLength={100} value={draft.title} onChange={e=>set('title',e.target.value)}/></label>
    <Channels kind="Email" entries={draft.emails} change={v=>set('emails',v)}/>
    <Channels kind="Phone" entries={draft.phones} change={v=>set('phones',v)}/>
    {error && <p role="alert">{error}</p>}<footer><button type="button" disabled={busy} onClick={close}>Cancel</button><button className="primary" disabled={busy}>Save contact</button></footer>
  </form></Modal>;
}

export function ActionModal({ action, contact, close, done }) {
  const password = action === 'password';
  const [error,setError] = useState('');
  const [busy,setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    const fields = Object.fromEntries(new FormData(e.currentTarget));
    if (password && fields.newPassword !== fields.confirm) { setError('The two new passwords must match.'); return; }
    setBusy(true); setError('');
    try {
      if (password) await api('/account/password','POST',{currentPassword:fields.currentPassword,newPassword:fields.newPassword});
      else await api('/contacts/' + contact.id,'DELETE');
      done();
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  return <Modal title={password ? 'Change password' : 'Delete contact?'} close={close} busy={busy}><form onSubmit={submit}>
    {password ? [['currentPassword','Current password'],['newPassword','New password'],['confirm','Confirm new password']].map(([name,label]) =>
      <label key={name}>{label}<input type="password" required name={name} maxLength={128} minLength={name === 'currentPassword' ? 1 : 10} autoComplete={name === 'currentPassword' ? 'current-password' : 'new-password'}/></label>) :
      <p>Remove {contact.firstName} {contact.lastName} from your address book?</p>}
    {error && <p role="alert">{error}</p>}<footer><button type="button" onClick={close} disabled={busy}>Cancel</button><button className={password ? 'primary' : 'danger'} disabled={busy}>{password ? 'Save new password' : 'Delete contact'}</button></footer>
  </form></Modal>;
}
