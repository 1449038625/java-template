// Tip: 告诉用户ie浏览器不支持
if (/MSIE\s|Trident\//.test(navigator.userAgent)) {
  document.body.innerHTML = "<strong>Sorry, this browser is currently not supported. We recommend using the latest version of a modern browser. For example, Chrome/Firefox/Edge.</strong>"
}
