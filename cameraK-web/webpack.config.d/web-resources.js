config.devServer = config.devServer || {};
config.devServer.static = {
    directory: require("path").resolve(
        __dirname,
        "../../../../cameraK-web/src/webMain/resources",
    ),
};
