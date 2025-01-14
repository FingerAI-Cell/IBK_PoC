/** @type {import('next').NextConfig} */
const nextConfig = {
    // React Strict Mode 활성화 (개발 중 오류를 잡기 위한 설정)
    reactStrictMode: true,
  
    // 빌드 시 ESLint 검사 무시 (필요한 경우 추가)
    eslint: {
      ignoreDuringBuilds: true,
    },
  
    // 이미지 최적화 (외부 도메인 허용 설정)
    images: {
      domains: [], // 여기에 허용할 외부 이미지 도메인을 추가하세요.
    },
  
    // 환경 변수 설정 (필요에 따라 정의)
    env: {
      NEXT_PUBLIC_API_URL: '', // API URL을 여기에 추가
    },
  
    // Webpack 사용자 정의 (특정 환경에 필요한 경우)
    webpack: (config, { isServer }) => {
      if (!isServer) {
        config.resolve.fallback = {
          fs: false,
          path: false,
        };
      }
      return config;
    },
  };
  
  module.exports = nextConfig;
  