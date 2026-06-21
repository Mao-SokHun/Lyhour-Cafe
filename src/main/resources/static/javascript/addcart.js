
document.addEventListener('DOMContentLoaded', () => {
  // --- Cart Logic ---

  // Initialize cart from localStorage or as an empty array
  let cart = JSON.parse(localStorage.getItem('cart')) || [];

  /**
   * Updates the cart count badge in the header.
   * The count is the total quantity of all items in the cart.
   */
  function updateCartCount() {
    const countEls = document.querySelectorAll('.cart-count-badge');
    if (countEls.length === 0) return;

    const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

    countEls.forEach(countEl => {
      if (totalItems > 0) {
        countEl.textContent = totalItems;
        countEl.classList.remove('hidden');
      } else {
        countEl.classList.add('hidden');
      }
    });
  }

  /**
   * Adds a product to the cart or updates its quantity if it already exists.
   * An item is considered the same if it has the same id, size, and sugar level.
   * @param {object} product - The product object to add.
   */
  function addToCart(product) {
    // Find if product already exists in cart
    const existingProductIndex = cart.findIndex(item => item.id === product.id);

    if (existingProductIndex > -1) {
      // Product exists, increment quantity
      cart[existingProductIndex].quantity += 1;
    } else {
      // Product is new, add it to cart with quantity 1
      cart.push({ ...product, quantity: 1 });
    }

    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartCount();
    if (typeof showToast === 'function') {
      showToast(`${product.name} added to your bag`);
    }
  }

  /**
   * Attaches click event listeners to all "Add to Cart" buttons.
   */
  document.querySelectorAll(".add-to-cart-btn").forEach((btn) => {
      btn.addEventListener("click", (event) => {
          event.preventDefault(); // Prevent link navigation
          event.stopPropagation(); // Stop event from bubbling up to parent elements

          const product = {
              id: btn.dataset.id,
              name: btn.dataset.name,
              price: parseFloat(btn.dataset.price),
              imageUrl: btn.dataset.imageUrl,
          };

          addToCart(product);
      });
  });

  // --- Initialization ---
  // Update cart count on page load
  updateCartCount();
});
