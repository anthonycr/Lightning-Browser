![](https://d4yt8xl9b7in.cloudfront.net/assets/home/logotype-heroku.png) Heroku CLI
==========

The Heroku CLI is used to manage Heroku apps from the command line.

For more about Heroku see <https://www.heroku.com/home>

To get started see <https://devcenter.heroku.com/start>

[![Build Status](https://travis-ci.org/heroku/heroku.svg?branch=master)](https://travis-ci.org/heroku/heroku)
[![Coverage Status](https://img.shields.io/coveralls/heroku/heroku.svg)](https://coveralls.io/r/heroku/heroku?branch=master)

Setup
-----

First, [Install the Heroku CLI with the Toolbelt](https://toolbelt.heroku.com).

Once installed, you'll have access to the `heroku` command from your command shell.  Log in using the email address and password you used when creating your Heroku account:

    $ heroku login
    Enter your Heroku credentials.
    Email: adam@example.com
    Password:
    Could not find an existing public key.
    Would you like to generate one? [Yn]
    Generating new SSH public key.
    Uploading SSH public key /Users/adam/.ssh/id_rsa.pub

Press enter at the prompt to upload your existing `SSH` key or create a new one, used for pushing code later on.

API
---

For additional information about the API see [Heroku API Quickstart](https://devcenter.heroku.com/articles/platform-api-quickstart) and [Heroku API Reference](https://devcenter.heroku.com/articles/platform-api-reference).

Meta
----

Released under the MIT license; see the file License.

Created by Adam Wiggins

[Other Contributors](https://github.com/heroku/heroku/contributors)
