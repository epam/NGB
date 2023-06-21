const ngbConstants = {
  env: process.env.__ENV__ || 'production',
  urlPrefix: process.env.__API_URL__ || '/'
}
export default ngbConstants;
