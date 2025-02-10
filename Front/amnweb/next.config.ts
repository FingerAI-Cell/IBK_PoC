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
  typescript: {
    ignoreBuildErrors: true,
  },
  transpilePackages: ['@react-pdf/renderer'],
  webpack: (config) => {
    config.resolve.alias = {
      ...config.resolve.alias,
      canvas: false,
      encoding: false
    }
    return config
  }
};

export default nextConfig;
