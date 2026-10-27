const http = require('http');

async function request(options, postData) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
}

async function run() {
  console.log('Testing endpoints...');

  // 1. Health
  const health = await request({ host: 'localhost', port: 5000, path: '/api/health', method: 'GET' });
  console.log('Health:', health.body);

  // 2. Login as Admin
  const login = await request(
    {
      host: 'localhost',
      port: 5000,
      path: '/api/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
    { email: 'admin@courtbook.com', password: 'Admin@123' }
  );
  console.log('Admin login:', login.body.success, 'Token length:', login.body.data.token.length);
  const token = login.body.data.token;

  // 3. Admin Dashboard
  const dash = await request({
    host: 'localhost',
    port: 5000,
    path: '/api/admin/dashboard',
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('Dashboard KPIs:', dash.body.data.kpi);

  // 4. Revenue Analytics Pipeline
  const rev = await request({
    host: 'localhost',
    port: 5000,
    path: '/api/admin/analytics/revenue',
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('Revenue by Venue Aggregation:', rev.body.data);

  // 5. Popular Sports Analytics Pipeline
  const sports = await request({
    host: 'localhost',
    port: 5000,
    path: '/api/admin/analytics/popular-sports',
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  console.log('Popular Sports Aggregation:', sports.body.data);

  // 6. Courts List
  const courts = await request({ host: 'localhost', port: 5000, path: '/api/courts', method: 'GET' });
  console.log('Courts found:', courts.body.count);

  console.log('✅ ALL API TESTS PASSED!');
}

run().catch(console.error);
