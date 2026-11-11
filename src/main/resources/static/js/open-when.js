const VALID_PASSWORDS = new Set(['OOR', 'Beagle', 'Beagle bark', 'Beagle sound', 'snoring','oor', 'beagle', 'beagle bark', 'beagle sound', ]);

function openLetter(letterId) {
    const userInput = prompt("What is your favorite sound:");

    if(VALID_PASSWORDS.has(userInput)) {
        // Save scroll position
        const scrollPosition = window.pageYOffset || document.documentElement.scrollTop;

        // Add no-scroll class to body
        document.body.classList.add('no-scroll');

        // Store scroll position as data attribute to restore later
        document.body.dataset.scrollPosition = scrollPosition;

        // Show the modal
        document.getElementById(letterId).style.display = "flex";
    } else {
        alert("Try again, mi vida ❤️");
    }
}

function closeModal(letterId) {
    // Hide the modal
    document.getElementById(letterId).style.display = "none";

    // Remove no-scroll class
    document.body.classList.remove('no-scroll');

    // Restore scroll position
    const scrollPosition = document.body.dataset.scrollPosition || 0;
    window.scrollTo(0, scrollPosition);
}

// Close modal when clicking outside content
window.onclick = function(event) {
    document.querySelectorAll('.letter-modal').forEach(modal => {
        if(event.target == modal) {
            closeModal(modal.id);
        }
    });
}

// Close modal with Escape key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        document.querySelectorAll('.letter-modal').forEach(modal => {
            if(modal.style.display === 'flex') {
                closeModal(modal.id);
            }
        });
    }
});