document.addEventListener('DOMContentLoaded', function() {
    // Gestion des menus déroulants au clic (plus accessible)
    const dropdowns = document.querySelectorAll('nav ul li.dropdown');

    dropdowns.forEach(dropdown => {
        const link = dropdown.querySelector('a');
        const dropdownContent = dropdown.querySelector('.dropdown-content');

        // Empêcher le lien principal de naviguer s'il a un sous-menu (optionnel)
        // if (dropdownContent) {
        //     link.addEventListener('click', function(event) {
        //         event.preventDefault();
        //         // Toggle display
        //         if (dropdownContent.style.display === 'block') {
        //             dropdownContent.style.display = 'none';
        //         } else {
        //             // Cacher les autres menus ouverts
        //             closeAllDropdowns();
        //             dropdownContent.style.display = 'block';
        //         }
        //     });
        // }
    });

    // Optionnel: Clic en dehors pour fermer les menus
    // document.addEventListener('click', function(event) {
    //     if (!event.target.closest('nav ul li.dropdown')) {
    //         closeAllDropdowns();
    //     }
    // });

    // function closeAllDropdowns() {
    //     document.querySelectorAll('nav ul .dropdown-content').forEach(content => {
    //         content.style.display = 'none';
    //     });
    // }

    // Pour le moment, la logique des menus déroulants est gérée par CSS (:hover)
    // Ce fichier est prêt pour des interactions plus complexes si nécessaire.
    console.log("JavaScript chargé et prêt.");

});
