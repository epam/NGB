/**
 * Signs the user out through the server's SAML single logout endpoint.
 *
 * This has to be a POST. The endpoint used to be reachable by any method, so both call sites simply
 * navigated to it; Spring Security 6's `saml2Logout()` matches `POST /saml/logout` and nothing else.
 * A GET now falls through to the plain logout filter, which clears the local session but never sends
 * a `<LogoutRequest>` - the user would still be signed in at the identity provider and would be
 * logged straight back in. CSRF is disabled on that filter chain, so the form needs no token.
 *
 * @param {string} logoutUrl absolute path of the SAML logout endpoint, e.g. `/catgenome/saml/logout`
 * @param {Document} [doc] document to build the form in; defaults to the ambient one
 */
export default function submitSamlLogout(logoutUrl, doc = document) {
    const form = doc.createElement('form');
    form.method = 'POST';
    form.action = logoutUrl;
    form.style.display = 'none';
    doc.body.appendChild(form);
    form.submit();
}
