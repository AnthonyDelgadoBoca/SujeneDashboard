import { ComplimentGenerator } from '/js/modules/compliments.js';
import { QuizManager } from '/js/modules/quiz.js';

document.addEventListener('DOMContentLoaded', () => {
    try {
        // Get the JSON data element
        const dataElement = document.getElementById('compliments-data');

        // Parse the JSON
        const compliments = dataElement ? JSON.parse(dataElement.textContent) : [];

        // Initialize modules
        new ComplimentGenerator('complimentBtn', 'complimentText', compliments);
        new QuizManager('quiz-answer', 'correct-answer', 'quizResult');
        new QuizManager('quiz-answer2', 'correct-answer2', 'quizResult2');
        new QuizManager('quiz-answer3', 'correct-answer3', 'quizResult3');
        new QuizManager('quiz-answer4', 'correct-answer4', 'quizResult4');
        new QuizManager('quiz-answer5', 'correct-answer5', 'quizResult5');
        new QuizManager('quiz-answer6', 'correct-answer6', 'quizResult6');
        new QuizManager('quiz-answer7', 'correct-answer7', 'quizResult7');
        new QuizManager('quiz-answer8', 'correct-answer8', 'quizResult8');
        new QuizManager('quiz-answer9', 'correct-answer9', 'quizResult9');
        new QuizManager('quiz-answer10', 'correct-answer10', 'quizResult10');
        new QuizManager('quiz-answer11', 'correct-answer11', 'quizResult11');
        new QuizManager('quiz-answer12', 'correct-answer12', 'quizResult12');
        new QuizManager('quiz-answer13', 'correct-answer13', 'quizResult13');



        console.log('Initialized successfully with compliments:', compliments);
    } catch (error) {
        console.error('Initialization error:', error);
        // Fallback compliments if there's an error
        new ComplimentGenerator('complimentBtn', 'complimentText', [
            "You're wonderful",
            "You're amazing"
        ]);
    }
});

// Reasons database
const reasons = [
  "How your eyes light up when you talk about things you love (beagle sounds)",
  "The comforting sound of your heartbeat when we hug or touch",
  "You make me want to be a better man everyday",
  "You are the light in my life",
  "I Love you",
  "I'll always want you",
  "I'll always want to take care of you",
  "something about yellow river phenotype here",
  "How happy am I to have you in my life",
  "How you make me want to kidnap you back to Florida",
  "I Love you so much",
  "I am probably missing you now",
  "How your smile melts away all my worries and makes the world feel right again",
  "The way your laugh fills the room and makes me fall in Love with you all over again",
  "Currently rubbing your nose",
  "The way you look at me makes me feel like the luckiest person alive",
  "I Love the way you challenge me and push me to grow, while always supporting me",
  "Your touch feels like electricity and comfort at the same time",
  "I Love that no matter how much time we spend together, it never feels like enough",
  "The way you believe in me, even when I doubt myself, makes me Love you more",
  "The thought of growing old with you fills me with peace and joy",
  "I Love the way you understand me without me even having to explain",
  "Your presence alone makes my heart feel full",
  "I Love how we can talk about everything and nothing, and it still feels meaningful",
];

// Create stars
function createStars() {
  const sky = document.querySelector('.night-sky');
  const starCount = 100;

  for (let i = 0; i < starCount; i++) {
    const star = document.createElement('div');
    star.className = 'star';

    // Random positioning
    star.style.left = `${Math.random() * 100}%`;
    star.style.top = `${Math.random() * 100}%`;
    star.style.setProperty('--duration', `${2 + Math.random() * 3}s`);

    // Random delay for twinkling
    star.style.animationDelay = `${Math.random() * 5}s`;

    // Click event
    star.addEventListener('click', () => {
      showRandomReason(star);
    });

    sky.appendChild(star);
  }
}

function showRandomReason(star) {
  const textElement = document.querySelector('.handwritten-text');
  const audio = document.getElementById('harpAudio');

  // Animate star
  star.style.transform = 'scale(3)';
  setTimeout(() => { star.style.transform = 'scale(1)'; }, 300);

  // Get random reason
  const randomReason = reasons[Math.floor(Math.random() * reasons.length)];

  // Display with typewriter effect
  let i = 0;
  textElement.textContent = '';
  const typing = setInterval(() => {
    if (i < randomReason.length) {
      textElement.textContent += randomReason.charAt(i);
      i++;
    } else {
      clearInterval(typing);
    }
  }, 50);

  // Play soft sound
  audio.currentTime = 0;
  audio.play();
}
function isInViewport(element) {
    const rect = element.getBoundingClientRect();
    return (
        rect.top <= (window.innerHeight || document.documentElement.clientHeight) &&
        rect.bottom >= 0
    );
}

// Add this function to handle scroll events
function handleScrollAnimation() {
    const constellation = document.querySelector('.constellation-container');
    if (isInViewport(constellation)) {
        constellation.classList.add('visible');
        // Remove the event listener after animation triggers
        window.removeEventListener('scroll', handleScrollAnimation);
    }
}
// Initialize on load
window.addEventListener('load', function() {
    createStars();
    // Add scroll event listener
    window.addEventListener('scroll', handleScrollAnimation);
    // Check immediately in case the element is already in view
    handleScrollAnimation();
});

// Heart-catching game implementation
document.addEventListener('DOMContentLoaded', function() {
    const canvas = document.getElementById('heart-game');
    const ctx = canvas.getContext('2d');
    const startButton = document.getElementById('start-game');
    const scoreDisplay = document.getElementById('game-score');
    const timerDisplay = document.getElementById('game-timer');
    const resultDisplay = document.getElementById('game-result');

    // Adjust canvas size for mobile
    if (window.innerWidth <= 768) {
        canvas.width = window.innerWidth - 40;
        canvas.height = 300;
    }

    let gameActive = false;
    let score = 0;
    let timeLeft = 30;
    let timer;
    let basket = { x: canvas.width / 2 - 30, width: 60 };
    let hearts = [];
    let touchX = null;

    // Heart class
    class Heart {
        constructor() {
            this.x = Math.random() * (canvas.width - 30);
            this.y = -30;
            this.size = 20 + Math.random() * 15;
            this.speed = 2 + Math.random() * 3;
            this.color = `hsl(${Math.random() * 360}, 100%, 65%)`;
        }

        update() {
            this.y += this.speed;
        }

        draw() {
            ctx.fillStyle = this.color;
            this.drawHeart(this.x, this.y, this.size);
        }

        drawHeart(x, y, size) {
            ctx.beginPath();
            ctx.moveTo(x, y);
            ctx.bezierCurveTo(
                x, y - size / 2,
                x - size, y - size / 2,
                x - size, y
            );
            ctx.bezierCurveTo(
                x - size, y + size / 3,
                x, y + size,
                x, y + size
            );
            ctx.bezierCurveTo(
                x, y + size,
                x + size, y + size / 3,
                x + size, y
            );
            ctx.bezierCurveTo(
                x + size, y - size / 2,
                x, y - size / 2,
                x, y
            );
            ctx.fill();
        }

        isCaught() {
            return this.y + this.size > canvas.height - 20 &&
                   this.x > basket.x &&
                   this.x < basket.x + basket.width;
        }
    }

    // Draw basket
    function drawBasket() {
        ctx.fillStyle = '#6f42c1';
        ctx.fillRect(basket.x, canvas.height - 20, basket.width, 10);
        ctx.fillStyle = '#5a32a3';
        ctx.fillRect(basket.x + 5, canvas.height - 25, basket.width - 10, 5);
    }

    // Game loop
    function gameLoop() {
        if (!gameActive) return;

        // Clear canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Draw basket
        drawBasket();

        // Create new hearts occasionally
        if (Math.random() < 0.05) {
            hearts.push(new Heart());
        }

        // Update and draw hearts
        for (let i = hearts.length - 1; i >= 0; i--) {
            hearts[i].update();
            hearts[i].draw();

            // Check if heart is caught
            if (hearts[i].isCaught()) {
                hearts.splice(i, 1);
                score++;
                scoreDisplay.textContent = `Hearts: ${score}`;
                continue;
            }

            // Remove hearts that fell off screen
            if (hearts[i].y > canvas.height) {
                hearts.splice(i, 1);
            }
        }

        requestAnimationFrame(gameLoop);
    }

    // Start game
    function startGame() {
        gameActive = true;
        score = 0;
        timeLeft = 30;
        hearts = [];
        scoreDisplay.textContent = `Hearts: ${score}`;
        timerDisplay.textContent = `Time: ${timeLeft}s`;
        resultDisplay.textContent = '';
        startButton.textContent = 'Playing...';
        startButton.disabled = true;

        // Start timer
        timer = setInterval(() => {
            timeLeft--;
            timerDisplay.textContent = `Time: ${timeLeft}s`;

            if (timeLeft <= 0) {
                endGame();
            }
        }, 1000);

        gameLoop();
    }

    // End game
    function endGame() {
        gameActive = false;
        clearInterval(timer);
        startButton.textContent = 'Play Again';
        startButton.disabled = false;

        // Display result message based on score
        let message = '';
        if (score < 30) {
            message = `You caught ${score} hearts! I still Love you, but maybe try again? 💖`;
        } else if (score < 55) {
            message = `You caught ${score} hearts! You've captured some of my Love! 💕`;
        } else if (score < 70) {
            message = `You caught ${score} hearts! You've captured most of my heart! 💞`;
        } else {
            message = `You caught ${score} hearts! You've captured all of my Love! I'm yours forever! 💘`;
        }

        resultDisplay.innerHTML = `<h4>Game Over!</h4><p>${message}</p>`;
    }

    // Mouse movement
    canvas.addEventListener('mousemove', (e) => {
        if (!gameActive) return;
        const rect = canvas.getBoundingClientRect();
        basket.x = e.clientX - rect.left - basket.width / 2;

        // Keep basket within canvas
        if (basket.x < 0) basket.x = 0;
        if (basket.x + basket.width > canvas.width) basket.x = canvas.width - basket.width;
    });

    // Touch movement for mobile
    canvas.addEventListener('touchmove', (e) => {
        if (!gameActive) return;
        e.preventDefault();
        const rect = canvas.getBoundingClientRect();
        touchX = e.touches[0].clientX - rect.left;
        basket.x = touchX - basket.width / 2;

        // Keep basket within canvas
        if (basket.x < 0) basket.x = 0;
        if (basket.x + basket.width > canvas.width) basket.x = canvas.width - basket.width;
    });

    // Start button event
    startButton.addEventListener('click', startGame);
});
