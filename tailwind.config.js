/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/main/resources/static/**/*.html',
    './src/main/resources/static/**/*.js',
    './src/main/resources/static/styles/input.css'
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: []
};
