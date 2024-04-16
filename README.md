![Password Manager for AuthMe](https://i.imgur.com/jDDh3AP.png)

# Password Manager for AuthMe (PWAM)

Password Manager aims to enhance security across servers utilizing the AuthMe plugin.

PWAM introduces a password management system that securely auto-fills passwords and generates strong, unique passwords as needed.

If you have any questions, please [join my Discord][Discord].

## Compatibility

This mod works with Minecraft 1.18.2, 1.19.4, 1.20.4 and 1.20.5-pre1.

Plugins like LoginSecurity, OpeNLogin, LibreLogin and UserLogin are also supported.

## Dependencies
Please install the following dependencies before you can use PWAM:

* [Fabric API](https://modrinth.com/mod/fabric-api)
* [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
* [SQLite JDBC](https://modrinth.com/plugin/sqlite-jdbc)

## How to use it?

Getting started with PWAM is straightforward, especially if you're already familiar with the autocomplete feature.

### TL;DR

Simply type */register* (or */login*, or any other command that requires a password) and press <kbd>Tab</kbd>.
PWAM will automatically suggest your password or generate a new one.

### Saving your existing password

To save your existing password, use the commands */login*, */l*, or */pwam set* as needed.
Your password will be securely stored for future use.

<details>
  <summary>See GIF</summary>
  
![login_2_1](https://i.imgur.com/2p1iN3y.gif)

</details>

### Registering on a new server

When registering on a new server, enter */register* (or */reg* if available).
PWAM will offer a securely generated password for you to use.
You can choose to use this suggestion or enter a password of your own.
Either way, PWAM will save your new password.

![reg_2](https://i.imgur.com/tybNrsd.gif)

### What if I do something wrong?

You can use globally accessible */pwam* command to look up your passwords (they are never deleted automatically!).
You can even use it in the singleplayer, if the server kicks you for standing too long.

<details>
  <summary>See GIF</summary>
  
![pwam_2](https://i.imgur.com/cdGI3Fc.gif)
  
</details>

[Discord]: https://discord.gg/EqhZfpwXmp
