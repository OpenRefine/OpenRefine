/*
 * when trailing slashes are added to URLs, this breaks internal links.
 * This is a fix for this issue taken from:
 * https://github.com/facebook/docusaurus/issues/2394#issuecomment-630638096
 */
if (window && window.location && window.location.pathname.endsWith('/') && window.location.pathname !== '/') {
  window.history.replaceState('', '', window.location.pathname.substr(0, window.location.pathname.length - 1) + window.location.hash)
}
