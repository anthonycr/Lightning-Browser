Heroku CLI Full Release Process
===============================

The following is how to do releases for OSX, Windows, and the "main" release (ubuntu, tgz, zip). If you are not a member of the CLI team and performing a release while they are out, you likely do not need to be concerned with this guide and can use the regular, much easier [release process](./RELEASE.md).

## OSX Release

Prerequisites:

* OSX
* Heroku Developer ID Installer Certificate in Keychain
  * `gpg --decrypt-files resources/pkg/certificate.p12.gpg`
    * Enter OSX .p12 Certificate password (LastPass Shared CLI Secure Note)
  * open resources/pkg/certificate.p12
    * There is no password on certificate.p12 (it was GPG encrypted instead)
  * rm resources/pkg/certificate.p12 (you do not need it anymore) 

* `HEROKU_RELEASE_ACCESS` and `HEROKU_RELEASE_SECRET`

To build for testing: `bundle exec rake pkg:build`. Outputs to `./dist/heroku-toolbelt-X.Y.Z.pkg`.
To release: `bundle exec rake pkg:release`.

## Windows Release

This is run not from a Windows machine, but from a UNIX machine with Wine.

Mac Prerequisites:

* Heroku Developer ID Installer Certificate in Keychain
* `HEROKU_RELEASE_ACCESS`, `HEROKU_RELEASE_SECRET`, `HEROKU_WINDOWS_SIGNING_PASS` (from LastPass)
* Install [XQuartz](http://xquartz.macosforge.org/) manually, or via the terminal (restart required):
* `brew install osslsigncode`

```sh
curl -O# http://xquartz-dl.macosforge.org/SL/XQuartz-2.7.6.dmg
hdiutil attach XQuartz-2.7.6.dmg -mountpoint /Volumes/xquartz
sudo installer -store -pkg /Volumes/xquartz/XQuartz.pkg -target /
hdiutil detach /Volumes/xquartz
rm XQuartz-2.7.6.dmg
```

* `/opt/X11/bin` should be in your `$PATH` so `Xvfb` can be started.
* Install wine: `brew install wine`
* The pfx file decrypted from `resources/exe/heroku-codesign-cert.pfx.gpg` (password in LastPass)
* Initialize wine: `bundle exec rake exe:init-wine`

To build for testing: `bundle exec rake exe:build`. Outputs to `./dist/heroku-toolbelt-X.Y.Z.exe`.
To release: `bundle exec rake exe:release`.

## Main Release

This process releases the tgz (standalone/homebrew), zip (for autoupdates), deb package and ruby gem. It's everything that is required to not end up with a partial release. This is what the buildserver does for you, so you shouldn't have to do this manually (this is just for reference). Because this builds a deb package, you must be on an Ubuntu box.

Prerequisites:

* Running from Ubuntu
* Make sure you have permissions to `heroku` gem through `gem` https://rubygems.org/gems/heroku.
* `HEROKU_RELEASE_ACCESS` and `HEROKU_RELEASE_SECRET`
* deb private key
* Ubuntu prerequisites:

```sh
echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | sudo debconf-set-selections
sudo apt-get install -y build-essential libpq-dev libsqlite3-dev curl xvfb wine
```

If this is your first time, you should first build the packages: `bundle exec rake build` Then look inside `./dist` to test each of the packages.

Once you are confident it works, release: `bundle exec rake release`. Note that release will automatically build if the packages are not there (there is no need to run `rake build`).

Note that you can look inside the `Rakefile` to test out each part of the step on your machine before it is built.

## Ruby versions

Toolbelt bundles Ruby using different sources according to the OS:

- Windows: fetches [rubyinstaller.exe](http://rubyinstaller.org/) from S3.
- Mac: fetches ruby.pkg from S3. That file was extracted from
[RailsInstaller](http://railsinstaller.org/en).
- Linux: uses system debs for Ruby.
