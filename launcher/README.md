# OpenRefine Launcher

This is a WIP work-in-progress for a Java module built with JavaFX

# Development Tools

## IDEs

### Intellij IDEA
Intellij IDEA is the recommended IDE for developing with JavaFX, but it is only a recommendation.

### VSCode
VSCode also has great support of JavaFX but only if certain extensions are also installed.

## Scene Builder

[Scene Builder](https://gluonhq.com/products/scene-builder/) is a free, open source editor for designing user interfaces with JavaFX.

IntelliJ IDEA and other IDEs allow you to open `.fxml` files in JavaFX Scene Builder right from the IDE after you specify the path to the Scene Builder application in the IDE's settings.

Learn how to use JavaFX Scene Builder from [JavaFX Scene Builder: User Guide](https://docs.oracle.com/javase/8/scene-builder-2/user-guide/index.html) on the Oracle website.

# Troubleshoot
- `Process finished with exit code -1073740791 (0xC0000409)`
  - The problem is caused by an error in the NVIDIA driver. Update your driver to the latest version. For more information, refer to the community forum.

- `"..." cannot be opened because the developer cannot be verified.`
  - The problem is caused by [notarization](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution) in macOS software. Follow the steps in this [discussion](https://github.com/hashicorp/terraform/issues/23033) to resolve the problem.