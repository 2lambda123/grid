const shared = require('./webpack.config.shared');
const { merge } = require('webpack-merge');
const ImageMinimizerPlugin = require("image-minimizer-webpack-plugin");

module.exports = merge(shared, {
  mode: 'production',
  devtool: 'source-map',
  optimization: {
    minimize: true,
    minimizer: [
      "...", // keep default minimizers (ie. js minimisation)
      new ImageMinimizerPlugin({
        minimizer: {
          implementation: ImageMinimizerPlugin.imageminMinify,
          options: {
            plugins: [
              // Svgo configuration here https://github.com/svg/svgo#configuration
              [
                "svgo",
                {
                  plugins: [
                    'preset-default',
                    {
                      name: "removeViewBox",
                      active: false,
                    },
                    {
                      name: "removeDimensions",
                      active: true,
                    },
                  ],
                },
              ],
            ]
          }
        }
      }),
    ]
  },
  resolve: {
    alias: {
      'react': 'preact/compat',
      'react-dom/test-utils': 'preact/test-utils',
      'react-dom': 'preact/compat',     // Must be below test-utils
      'react/jsx-runtime': 'preact/jsx-runtime'
    },
  }
});
