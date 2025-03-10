export default {
    content: [
      "./index.html",
      "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
      extend: {
        colors: {
          todo: {
            'done': '#4CAF50',    // Green color for done buttons
            'delete': '#F44336',  // Red color for delete buttons
            'button': '#FFEB3B',  // Yellow color for general buttons
          }
        }
      },
    },
    plugins: [],
  }