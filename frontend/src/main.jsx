import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api } from './api';
import { Auth, Modal, ContactEditor, ActionModal } from './forms';
import './style.css';

function Details({ contact, close, edit, remove }) {
  return <Modal title={contact.firstName + ' ' + contact.lastName} close={close}>
    {contact.title && <p>{contact.title}</p>}
    {[['Email addresses',contact.emails],['Phone numbers',contact.phones]].map(([title,entries]) =>
      <section key={title}><h3>{title}</h3>{entries.length ? <dl>{entries.map((e,i)=><React.Fragment key={i}><dt>{e.label}</dt><dd>{e.value}</dd></React.Fragment>)}</dl> : <p>None added</p>}</section>)}
    <footer><button className="delete-text" onClick={remove}>Delete</button><button className="primary" onClick={edit}>Edit contact</button></footer>
  </Modal>;
}

function Dashboard({ user, signedOut }) {
  const [screen,setScreen] = useState('contacts');
  const [query,setQuery] = useState('');
  const [page,setPage] = useState(0);
  const [revision,setRevision] = useState(0);
  const [result,setResult] = useState(null);
  const [error,setError] = useState('');
  const [notice,setNotice] = useState('');
  const [modal,setModal] = useState(null);
  useEffect(() => {
    if (screen !== 'contacts') return;
    const controller = new AbortController();
    setResult(null); setError('');
    api('/contacts?' + new URLSearchParams({q:query,page}), 'GET', null, controller.signal)
      .then(data => {
        if (controller.signal.aborted) return;
        if (page > 0 && page >= data.pages) setPage(Math.max(0,data.pages-1));
        else setResult(data);
      }).catch(err=>{if (!controller.signal.aborted) setError(err.message);});
    return () => controller.abort();
  },[screen,query,page,revision]);
  const close = () => setModal(null);
  function saved(message) { close(); setNotice(message); setRevision(r=>r+1); }
  async function logout() {
    try { await api('/account/logout','POST'); signedOut(); } catch (err) { setError(err.message); }
  }
  function navigate(next) { setScreen(next); setNotice(''); setError(''); close(); }
  return <><header><div className="brand"><span>✽</span> Address Book</div><nav aria-label="Account navigation">
    <button aria-current={screen==='contacts' ? 'page' : undefined} onClick={()=>navigate('contacts')}>Contacts</button>
    <button aria-current={screen==='profile' ? 'page' : undefined} onClick={()=>navigate('profile')}>My profile</button></nav></header>
    <main className="dashboard">{notice && <p role="status" className="notice">{notice}</p>}
    {error && <p role="alert">{error} <button onClick={()=>setRevision(r=>r+1)}>Retry</button></p>}
    {screen === 'profile' ? <section className="profile"><h1>My profile</h1><dl><dt>Name</dt><dd>{user.name}</dd><dt>Sign-in identifier</dt><dd>{user.identifier}</dd></dl>
      <footer><button onClick={()=>setModal({action:'password'})}>Change password</button><button onClick={logout}>Log out</button></footer></section> :
    <><div className="title-row"><div><h1>Contacts</h1><p>Your personal address book</p></div><button className="primary" onClick={()=>setModal({action:'create'})}>+ New contact</button></div>
      <label className="search">Search contacts<input type="search" placeholder="Search by first or last name" maxLength={160} value={query} onChange={e=>{setQuery(e.target.value);setPage(0);}}/></label>
      {result ? <><div className="count">{result.total} {result.total === 1 ? 'contact' : 'contacts'}</div>
        {result.items.length ? <div className="directory">{result.items.map(c=><article className="contact" key={c.id}>
          <span className="avatar" aria-hidden="true">{c.firstName[0]}{c.lastName[0]}</span>
          <div className="person"><button className="person-name" onClick={()=>setModal({action:'detail',contact:c})}>{c.firstName} {c.lastName}</button><small>{c.title}</small></div>
          <span className="channel">{c.emails[0]?.value || c.phones[0]?.value || '—'}</span>
          <div className="tools"><button onClick={()=>setModal({action:'edit',contact:c})}>Edit</button><button className="delete-text" onClick={()=>setModal({action:'delete',contact:c})}>Delete</button></div>
        </article>)}</div> : <section className="empty"><h2>{query ? 'No matching contacts' : 'Add your first contact'}</h2><p>{query ? 'Try a different first or last name.' : 'Select New contact to save someone to your address book.'}</p></section>}
        {result.pages > 1 && <div className="pagination"><button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Previous</button> Page {page+1} of {result.pages} <button disabled={page+1>=result.pages} onClick={()=>setPage(p=>p+1)}>Next</button></div>}
      </> : !error && <p role="status">Loading contacts…</p>}</>}
    </main>
    {modal?.action==='detail' && <Details contact={modal.contact} close={close} edit={()=>setModal({...modal,action:'edit'})} remove={()=>setModal({...modal,action:'delete'})}/>}
    {['create','edit'].includes(modal?.action) && <ContactEditor contact={modal.contact} close={close} saved={()=>saved('Contact saved.')}/>}
    {['password','delete'].includes(modal?.action) && <ActionModal {...modal} close={close} done={()=>saved(modal.action==='delete' ? 'Contact deleted.' : 'Password changed.')}/>}
  </>;
}

function App() {
  const [user,setUser] = useState(undefined);
  const [error,setError] = useState('');
  const [attempt,setAttempt] = useState(0);
  useEffect(()=>{
    let active = true;
    const ended = ()=>setUser(null);
    window.addEventListener('session-ended',ended);
    api('/account').then(data=>{if(active)setUser(data);}).catch(err=>{
      if(active && err.status!==401) setError('Cannot reach the contact service. Check that the backend is running.');
    });
    return ()=>{active=false;window.removeEventListener('session-ended',ended);};
  },[attempt]);
  if (error) return <main className="dashboard"><p role="alert">{error}</p><button onClick={()=>{setError('');setAttempt(a=>a+1);}}>Retry</button></main>;
  if (user===undefined) return <main className="dashboard" role="status">Opening your address book…</main>;
  return user ? <Dashboard user={user} signedOut={()=>setUser(null)}/> : <Auth onLogin={setUser}/>;
}
createRoot(document.getElementById('root')).render(<App/>);
