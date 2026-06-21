document.addEventListener('DOMContentLoaded', () => {
  const header = document.getElementById('header');
  if (header) {
    const onScroll = () => header.classList.toggle('is-scrolled', window.scrollY > 24);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
  }

  const menuToggle = document.getElementById('menu-toggle');
  const mobileMenu = document.getElementById('mobile-menu');
  if (menuToggle && mobileMenu) {
    menuToggle.addEventListener('click', () => mobileMenu.classList.toggle('open'));
  }

  const profileBtn = document.getElementById('profile-toggle-btn');
  const profileMenu = document.getElementById('profile-menu');
  if (profileBtn && profileMenu) {
    profileBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      profileMenu.classList.toggle('open');
    });
    window.addEventListener('click', () => profileMenu.classList.remove('open'));
  }

  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const category = btn.dataset.category;
      document.querySelectorAll('.tab-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.product-card').forEach((card) => {
        const match = category === 'All' || card.dataset.category === category;
        card.classList.toggle('hidden', !match);
      });
    });
  });

  const adminToggle = document.getElementById('admin-sidebar-toggle');
  const adminSidebar = document.getElementById('admin-sidebar');
  if (adminToggle && adminSidebar) {
    adminToggle.addEventListener('click', () => adminSidebar.classList.toggle('open'));
  }
});

function showToast(message, type = 'success') {
  let stack = document.getElementById('toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.id = 'toast-stack';
    stack.className = 'toast-stack';
    stack.setAttribute('aria-live', 'polite');
    document.body.appendChild(stack);
  }

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.innerHTML = `
    <span class="toast-icon"><i class="fa-solid fa-check"></i></span>
    <span>${message}</span>
  `;
  stack.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('is-leaving');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  }, 2800);
}

window.showToast = showToast;
