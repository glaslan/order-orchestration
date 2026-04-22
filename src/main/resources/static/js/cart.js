/**
 * CART PAGE LOGIC
 */

document.addEventListener("DOMContentLoaded", () => {
  // Initialize UI states
  updateTotals();
  document.querySelectorAll('input[name="quantity"]').forEach((input) => {
    syncButtons(input);
  });

  // Handle click events for +/- buttons and Trash button
  document.querySelectorAll(".input-group").forEach((group) => {
    const btnMinus = group.querySelector('input[value="-"]');
    const btnPlus = group.querySelector('input[value="+"]');
    const input = group.querySelector('input[name="quantity"]');
    const form = group.closest("form");
    if (!input || !form) return;

    const itemIdInput = form.querySelector('input[name="itemId"]');
    const itemId = (itemIdInput || {}).value;

    const trashBtn = form.querySelector("a.btn-danger");
    if (trashBtn) {
      trashBtn.addEventListener("click", async (e) => {
        e.preventDefault();
        try {
          const currentQty = parseInt(input.value);
          const body = new URLSearchParams({ itemId, quantity: currentQty });
          const res = await fetch("/removeFromCart", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body,
          });
          if (!res.ok) throw new Error(await res.text());
          removeRowFromDOM(itemId);
          updateTotals();
          showToast("Item removed from cart");
        } catch (err) {
          showToast("Error: " + err.message, "danger");
        }
      });
    }

    if (btnPlus) {
      btnPlus.addEventListener("click", async () => {
        const max = parseInt(input.getAttribute("max") || "9999");
        const cur = parseInt(input.value);
        if (cur >= max) return;

        try {
          const body = new URLSearchParams({ itemId, quantity: 1 });
          const res = await fetch("/addToCart", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body,
          });
          if (!res.ok) throw new Error(await res.text());
          input.value = cur + 1;
          updatePriceCell(input);
          input.dataset.initial = cur + 1;
          syncButtons(input);
          updateTotals();
          showToast("Quantity updated");
        } catch (err) {
          showToast("Error: " + err.message, "danger");
        }
      });
    }

    if (btnMinus) {
      btnMinus.addEventListener("click", async () => {
        const min = parseInt(input.getAttribute("min") || "1");
        const cur = parseInt(input.value);
        if (cur <= min) return;

        try {
          const body = new URLSearchParams({ itemId, quantity: 1 });
          const res = await fetch("/removeFromCart", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body,
          });
          if (!res.ok) throw new Error(await res.text());
          input.value = cur - 1;
          updatePriceCell(input);
          input.dataset.initial = cur - 1;
          syncButtons(input);
          updateTotals();
          showToast("Quantity updated");
        } catch (err) {
          showToast("Error: " + err.message, "danger");
        }
      });
    }
  });

  // Manual quantity input handler
  document.querySelectorAll('input[name="quantity"]').forEach((input) => {
    input.addEventListener("change", async function (e) {
      let val = parseInt(this.value);
      const max = parseInt(this.getAttribute("max") || "9999");
      const min = parseInt(this.getAttribute("min") || "1");
      const initial = parseInt(
        this.dataset.initial || this.getAttribute("value") || min,
      );

      if (isNaN(val) || val < min) {
        val = min;
        this.value = val;
        showToast(`Quantity must be at least ${min}`, "warning");
        return;
      }
      if (val > max) {
        showToast(`Only ${max} in stock`, "danger");
        this.value = initial;
        return;
      }

      const form = this.closest("form");
      if (!form) return;

      const delta = val - initial;
      if (delta === 0) return;

      try {
        const itemId = form.querySelector('input[name="itemId"]')?.value;
        if (!itemId) return;

        const url = delta > 0 ? "/addToCart" : "/removeFromCart";
        const body = new URLSearchParams({
          itemId,
          quantity: Math.abs(delta),
        });

        const res = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body,
        });

        if (!res.ok) throw new Error(await res.text());

        this.dataset.initial = val;
        syncButtons(this);
        updatePriceCell(this);
        updateTotals();
        showToast("Quantity updated", "success");
      } catch (err) {
        showToast(err.message, "danger");
        this.value = initial;
      }
    });

    input.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        this.blur();
      }
    });
  });
});

function updatePriceCell(input) {
  const row = input.closest(".row");
  if (!row) return;

  const cols = row.querySelectorAll(".col");
  if (cols.length < 2) return;

  const priceCell = cols[1];
  const priceText = priceCell.textContent
    .trim()
    .replace("$", "")
    .replace(" CAD", "");
  const qty = parseInt(input.value) || 0;
  const oldQty = parseInt(input.dataset.initial) || 1;
  const unitPrice = parseFloat(priceText) / oldQty || 0;

  priceCell.textContent = "$" + (unitPrice * qty).toFixed(2);
}

function updateTotals() {
  let subtotal = 0;
  document.querySelectorAll('input[name="quantity"]').forEach((input) => {
    const row = input.closest(".row");
    if (!row) return;
    const priceCell = row.querySelectorAll(".col")[1];
    if (priceCell) {
      const priceText = priceCell.textContent
        .trim()
        .replace("$", "")
        .replace(" CAD", "");
      subtotal += parseFloat(priceText) || 0;
    }
  });

  const taxesAndFees = 8.8;
  const total = subtotal + taxesAndFees;

  const subtotalEl = document.getElementById("subtotal-value");
  const totalEl = document.getElementById("total-value");

  if (subtotalEl) subtotalEl.textContent = "$" + subtotal.toFixed(2) + " CAD";
  if (totalEl) totalEl.textContent = "$" + total.toFixed(2) + " CAD";
}

function syncButtons(input) {
  const group = input.closest(".input-group");
  if (!group) return;
  const btnMinus = group.querySelector('input[value="-"]');
  const btnPlus = group.querySelector('input[value="+"]');
  const val = parseInt(input.value);
  const min = parseInt(input.getAttribute("min") || "1");
  const max = parseInt(input.getAttribute("max") || "9999");
  if (btnMinus) btnMinus.disabled = val <= min;
  if (btnPlus) btnPlus.disabled = val >= max;
}

function removeRowFromDOM(itemId) {
  document.querySelectorAll('input[name="itemId"]').forEach((idInput) => {
    if (idInput.value === String(itemId)) {
      const row = idInput.closest(".row");
      if (row) {
        const hr = row.previousElementSibling;
        if (hr && hr.tagName === "HR") hr.remove();
        row.remove();
      }
    }
  });
}

function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast align-items-center text-white bg-${type} border-0 mb-2`;
  toast.setAttribute("role", "alert");
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  `;
  container.prepend(toast);

  if (window.bootstrap && bootstrap.Toast) {
    new bootstrap.Toast(toast, { delay: 2400 }).show();
    toast.addEventListener("hidden.bs.toast", () => toast.remove());
  } else {
    setTimeout(() => toast.remove(), 2500);
  }
}
