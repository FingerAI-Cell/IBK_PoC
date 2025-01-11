import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  basePath: '',
  eslint: {
    ignoreDuringBuilds: true,
  },
  logging: {
    fetches: {
      fullUrl: true,
    },
  },
  crossOrigin: 'anonymous',
};

export default nextConfig;
