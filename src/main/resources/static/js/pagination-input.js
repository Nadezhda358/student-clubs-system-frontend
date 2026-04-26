(function () {
  function init(root) {
    const scope = root instanceof Element ? root : document;
    const forms = [];

    if (scope.matches && scope.matches("[data-page-input]")) {
      forms.push(scope);
    }

    scope.querySelectorAll?.("[data-page-input]").forEach((form) => {
      if (!forms.includes(form)) {
        forms.push(form);
      }
    });

    forms.forEach((form) => {
      if (!(form instanceof HTMLFormElement) || form.dataset.pageInputReady === "true") {
        return;
      }

      form.dataset.pageInputReady = "true";
      const pageInput = form.querySelector("input[name='pageDisplay']");
      if (!(pageInput instanceof HTMLInputElement)) {
        return;
      }

      const hiddenPage =
        form.querySelector("input[name='page']") ||
        Object.assign(document.createElement("input"), {
          type: "hidden",
          name: "page",
        });

      if (!hiddenPage.parentNode) {
        form.appendChild(hiddenPage);
      }

      const normalizePage = () => {
        const raw = Number.parseInt(pageInput.value, 10);
        const max = Number.parseInt(pageInput.max || "1", 10);
        const safeMax = Number.isNaN(max) || max < 1 ? 1 : max;
        const safeValue = Number.isNaN(raw) ? 1 : Math.min(Math.max(raw, 1), safeMax);
        pageInput.value = String(safeValue);
        hiddenPage.value = String(safeValue - 1);
      };

      pageInput.addEventListener("change", () => {
        normalizePage();
        if (form.requestSubmit) {
          form.requestSubmit();
        } else {
          form.submit();
        }
      });

      form.addEventListener("submit", () => {
        normalizePage();
      });
    });
  }

  window.ClubsHubPaginationInput = { init };
  document.addEventListener("DOMContentLoaded", () => init(document));
})();
