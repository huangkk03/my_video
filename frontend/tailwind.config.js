export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#00AEEC',
        secondary: '#F1F2F3',
      },
      borderRadius: {
        'lg': '8px',
        'xl': '12px',
      }
    },
  },
  plugins: [],
}