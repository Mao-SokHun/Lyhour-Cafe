document.addEventListener('DOMContentLoaded', () => {
    const header = document.getElementById('header');
    if (header) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 50) {
                header.classList.add('bg-white', 'bg-opacity-70', 'backdrop-blur-md', 'shadow-md');
                header.querySelectorAll('nav.text-white,a.text-white, svg.text-white, div.text-white').forEach(el => {
                    el.classList.replace('text-white', 'text-black');
                });
            } else {
                header.classList.remove('bg-white', 'bg-opacity-70', 'backdrop-blur-md', 'shadow-md');
                header.querySelectorAll('nav.text-black,a.text-black, svg.text-black, div.text-black').forEach(el => {
                    el.classList.replace('text-black', 'text-white');
                });
            }
        });
    }
});