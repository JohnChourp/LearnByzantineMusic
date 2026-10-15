// Light/dark switch. Without JavaScript the page follows the system theme through CSS alone.
// The choice is remembered only in this browser (localStorage); nothing leaves the device.
(function () {
  var root = document.documentElement;
  var KEY = "lbm-theme";

  function stored() {
    try {
      return localStorage.getItem(KEY);
    } catch (e) {
      return null;
    }
  }

  function systemDark() {
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  }

  function current() {
    return root.getAttribute("data-theme") || (systemDark() ? "dark" : "light");
  }

  var saved = stored();
  if (saved === "light" || saved === "dark") {
    root.setAttribute("data-theme", saved);
  }

  document.addEventListener("DOMContentLoaded", function () {
    var button = document.querySelector(".theme-toggle");
    if (!button) return;

    // The label stays "Dark theme"; aria-pressed says whether it is on.
    function render() {
      button.setAttribute("aria-pressed", String(current() === "dark"));
    }

    button.addEventListener("click", function () {
      var next = current() === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", next);
      try {
        localStorage.setItem(KEY, next);
      } catch (e) {
        // Private mode or blocked storage: the switch still works for this visit.
      }
      render();
    });

    button.hidden = false;
    render();
  });
})();
