document.addEventListener("DOMContentLoaded", () => {
  fetch("/api/products/impact").then(response => response.ok ? response.json() : Promise.reject()).then(data => {
    [["about-saved", data.savedCount], ["about-social", data.socialCount], ["about-processing", data.processingCount]].forEach(([id, value]) => {
      const node = document.getElementById(id), target = Number(value || 0), start = 0, duration = 750, began = performance.now();
      const animate = now => {
        const progress = Math.min(1, (now - began) / duration);
        node.textContent = String(Math.round(start + target * progress));
        if (progress < 1) requestAnimationFrame(animate);
      };
      requestAnimationFrame(animate);
    });
  }).catch(() => {});
});