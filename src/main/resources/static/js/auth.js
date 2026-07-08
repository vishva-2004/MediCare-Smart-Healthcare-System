// ===== MediCare shared auth helper =====
const MediCare = (function(){
  function token(){ return localStorage.getItem('jwtToken'); }
  function role(){ return localStorage.getItem('role'); }
  function fullName(){ return localStorage.getItem('fullName') || 'User'; }
  function username(){ return localStorage.getItem('username') || ''; }

  function logout(){
    localStorage.clear();
    document.cookie = 'jwtToken=; path=/; max-age=0';
    window.location.href = '/login';
  }

  function requireLogin(){
    if (!token()) { window.location.href = '/login'; return false; }
    return true;
  }

  function requireRole(r){
    if (!requireLogin()) return false;
    if (role() !== r) { window.location.href = '/login'; return false; }
    return true;
  }

  async function api(url, options){
    options = options || {};
    options.headers = Object.assign({
      'Content-Type':'application/json',
      'Authorization':'Bearer ' + (token() || '')
    }, options.headers || {});
    try {
      const res = await fetch(url, options);
      if (res.status === 401 || res.status === 403) { logout(); return null; }
      return res;
    } catch(e) {
      return null;
    }
  }

  function renderTopbar(active){
    const el = document.getElementById('topbar');
    if (!el) return;
    const r = role();
    let links = '';
    if (r === 'ADMIN') {
      links = `
        <a href="/dashboard"     class="${active==='dashboard'?'active':''}">Dashboard</a>
        <a href="/doctors"       class="${active==='doctors'?'active':''}">Doctors</a>
        <a href="/patients"      class="${active==='patients'?'active':''}">Patients</a>
        <a href="/appointments"  class="${active==='appointments'?'active':''}">Appointments</a>
        <a href="/medical-records" class="${active==='records'?'active':''}">Records</a>`;
    } else if (r === 'DOCTOR') {
      links = `
        <a href="/doctor-dashboard" class="${active==='dashboard'?'active':''}">Dashboard</a>
        <a href="/appointments"     class="${active==='appointments'?'active':''}">Appointments</a>
        <a href="/medical-records"  class="${active==='records'?'active':''}">Records</a>
        <a href="/change-password"  class="${active==='change-password'?'active':''}">Password</a>`;
    } else {
      links = `
        <a href="/patient-dashboard" class="${active==='dashboard'?'active':''}">Dashboard</a>
        <a href="/symptom-checker"   class="${active==='symptoms'?'active':''}">Symptom Checker</a>
        <a href="/appointments"      class="${active==='appointments'?'active':''}">My Appointments</a>
        <a href="/medical-records"   class="${active==='records'?'active':''}">My Records</a>
        <a href="/change-password"   class="${active==='change-password'?'active':''}">Password</a>`;
    }
    el.innerHTML = `
      <div class="topbar-inner">
        <a href="/home" class="brand"><span class="brand-mark">+</span>Medi<span>Care</span></a>
        <div class="nav-links">
          ${links}
          <span style="color:var(--muted);font-size:13px;margin:0 6px">Hi, ${fullName()}</span>
          <a href="#" onclick="MediCare.logout();return false" class="btn btn-outline" style="padding:6px 14px;font-size:13px">Logout</a>
        </div>
      </div>`;
  }

  return { token, role, fullName, username, logout, requireLogin, requireRole, api, renderTopbar };
})();
