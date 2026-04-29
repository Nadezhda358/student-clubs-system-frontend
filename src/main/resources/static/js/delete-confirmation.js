(function () {
  const modal = document.getElementById("deleteConfirmModal");
  if (!modal) {
    return;
  }

  const modalDialog = modal.querySelector("[role='dialog']");
  const modalMessage = document.getElementById("deleteConfirmMessage");
  const modalCancel = document.getElementById("deleteConfirmCancel");
  const modalConfirm = document.getElementById("deleteConfirmConfirm");
  const modalCloseButtons = modal.querySelectorAll("[data-delete-modal-close]");
  const deleteForms = document.querySelectorAll("form[data-delete-confirm]");

  let pendingForm = null;
  let lastFocusedElement = null;

  deleteForms.forEach((form) => {
    form.addEventListener("submit", function (event) {
      if (form.dataset.deleteConfirmed === "true") {
        delete form.dataset.deleteConfirmed;
        return;
      }

      event.preventDefault();
      pendingForm = form;
      lastFocusedElement = event.submitter instanceof HTMLElement ? event.submitter : document.activeElement;

      if (modalMessage) {
        modalMessage.textContent = "Сигурни ли сте, че искате да изтриете този елемент?";
      }

      if (modalConfirm instanceof HTMLButtonElement) {
        const submitterLabel = getSubmitterLabel(event.submitter);
        modalConfirm.textContent = submitterLabel || "Изтрий";
      }

      openModal();
    });
  });

  modalCancel?.addEventListener("click", function () {
    closeModal();
  });

  modalCloseButtons.forEach((button) => {
    button.addEventListener("click", function () {
      closeModal();
    });
  });

  modal?.addEventListener("click", function (event) {
    if (event.target === modal) {
      closeModal();
    }
  });

  modalConfirm?.addEventListener("click", function () {
    if (!pendingForm) {
      closeModal();
      return;
    }

    pendingForm.dataset.deleteConfirmed = "true";
    pendingForm.submit();
    closeModal(false);
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && !modal.hidden) {
      closeModal();
    }
  });

  function openModal() {
    modal.hidden = false;
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("delete-modal-open");
    document.addEventListener("keydown", handleModalKeydown);

    if (modalDialog instanceof HTMLElement) {
      modalDialog.focus();
    } else {
      modalConfirm?.focus();
    }
  }

  function closeModal(restoreFocus = true) {
    modal.classList.remove("is-open");
    modal.hidden = true;
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("delete-modal-open");

    pendingForm = null;
    document.removeEventListener("keydown", handleModalKeydown);

    if (restoreFocus && lastFocusedElement instanceof HTMLElement) {
      lastFocusedElement.focus();
    }
  }

  function handleModalKeydown(event) {
    if (modal.hidden) {
      return;
    }

    if (event.key === "Escape") {
      closeModal();
      return;
    }

    if (event.key !== "Tab") {
      return;
    }

    const focusable = [modalCloseButtons[0], modalCancel, modalConfirm].filter(
      (node) => node instanceof HTMLElement && !node.hasAttribute("disabled")
    );

    if (focusable.length === 0) {
      return;
    }

    const firstFocusable = focusable[0];
    const lastFocusable = focusable[focusable.length - 1];

    if (event.shiftKey && document.activeElement === firstFocusable) {
      event.preventDefault();
      lastFocusable.focus();
      return;
    }

    if (!event.shiftKey && document.activeElement === lastFocusable) {
      event.preventDefault();
      firstFocusable.focus();
    }
  }

  function getSubmitterLabel(submitter) {
    if (!(submitter instanceof HTMLElement)) {
      return "";
    }

    return (submitter.textContent || "").trim();
  }
})();
