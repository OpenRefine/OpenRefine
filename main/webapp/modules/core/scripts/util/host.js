class Host {
  static isLocalhost() {
    const localhosts = ['localhost', '127.0.0.1', '::1'];
    return localhosts.includes(window.location.hostname);
  }
}