import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

// 后端服务地址（接口前缀 /api 由各环境 VITE_APP_BASE_API 提供）：
//   本地网关   : http://192.168.0.26:8086，base 用 /dev-api
//   远程接口   : https://test-ai.neucityai.com，支持 /neucity/api 和 /test_neucity/api
const localTarget = 'http://192.168.0.26:8086'
const remoteTarget = 'https://test-ai.neucityai.com'
const fileTarget = remoteTarget

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/build/' : '/build/',
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      port: 90,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        // WebSocket 专用代理：与 HTTP 代理分开，避免 ws:true 和 multipart HTTP 挤在同一 context 互相影响
        '/neucity/api/audioVideo': {
          target: remoteTarget,
          changeOrigin: true,
          secure: false,
          ws: true
        },
        '/test_neucity/api/audioVideo': {
          target: remoteTarget,
          changeOrigin: true,
          secure: false,
          ws: true
        },
        // 远程 staging HTTP：VITE_APP_BASE_API=/neucity/api
        '/neucity': {
          target: remoteTarget,
          changeOrigin: true,
          secure: false
        },
        // 远程 development/production HTTP：VITE_APP_BASE_API=/test_neucity/api
        '/test_neucity': {
          target: remoteTarget,
          changeOrigin: true,
          secure: false
        },
        // 本地后端：把 VITE_APP_BASE_API 改成 /dev-api 即走本地网关
        '/dev-api': {
          target: localTarget + '/api',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
        // 文件服务：用于本地开发访问 /file/**，避免浏览器直接 fetch 外域文件触发 CORS
        '/file': {
          target: fileTarget,
          changeOrigin: true,
          secure: false
        },
         // springdoc proxy
         '^/v3/api-docs/(.*)': {
          target: remoteTarget,
          changeOrigin: true,
          secure: false
        }
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
