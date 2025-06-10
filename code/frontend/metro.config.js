const { getDefaultConfig } = require("expo/metro-config");
const { withNativeWind } = require("nativewind/metro");

// Obter a configuração padrão
const config = getDefaultConfig(__dirname);

// Adicionar o polyfill para 'events' no resolver
config.resolver.extraNodeModules = {
  ...(config.resolver.extraNodeModules || {}),
  events: require.resolve("events/"),
};

// Exportar a configuração com nativewind
module.exports = withNativeWind(config, { input: "./global.css" });
