import colors from 'tailwindcss/colors';

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#E8F4FB',
          100: '#D1E9F7',
          200: '#A3D3F0',
          300: '#75BDE8',
          400: '#4FB8F5',
          500: '#1E7FC4',
          600: '#18659D',
          700: '#124B75',
          800: '#0F3D6E',
          900: '#092543',
        },
        accent: {
          light: '#4FB8F5',
          DEFAULT: '#1E7FC4',
          dark: '#0F3D6E',
        },
        pharma: {
          navy: '#0F3D6E',
          primary: '#1E7FC4',
          accent: '#4FB8F5',
          tint: '#E8F4FB',
          green: '#1B6B3A',
          gray: '#5F5E5A',
          light: '#F1EFE8',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
};
