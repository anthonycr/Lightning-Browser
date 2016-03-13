require "spec_helper"
require "heroku/auth"
require "heroku/helpers"

module Heroku
  describe Auth do
    include Heroku::Helpers

    before do
      ENV['HEROKU_API_KEY'] = nil

      @cli = Heroku::Auth
      allow(@cli).to receive(:check)
      allow(@cli).to receive(:display)
      allow(@cli).to receive(:running_on_a_mac?).and_return(false)
      @cli.credentials = nil

      FakeFS.activate!

      allow(FakeFS::File).to receive(:stat).and_return(double('stat', :mode => "0600".to_i(8)))
      allow(FakeFS::FileUtils).to receive(:chmod)
      allow(FakeFS::File).to receive(:readlines) do |path|
        File.read(path).split("\n").map {|line| "#{line}\n"}
      end

      allow(Heroku::Auth).to receive(:home_directory).and_return(Heroku::Helpers.home_directory)
      FileUtils.mkdir_p(@cli.netrc_path.split("/")[0..-2].join("/"))

      File.open(@cli.netrc_path, "w") do |file|
        file.puts("machine api.heroku.com\n  login user\n  password pass\n")
        file.puts("machine git.heroku.com\n  login user\n  password pass\n")
      end
    end

    after do
      FileUtils.rm_rf(@cli.netrc_path)
      FakeFS.deactivate!
    end

    context "legacy credentials" do

      before do
        FileUtils.rm_rf(@cli.netrc_path)
        FileUtils.mkdir_p(File.dirname(@cli.legacy_credentials_path))
        File.open(@cli.legacy_credentials_path, "w") do |file|
          file.puts "legacy_user\nlegacy_pass"
        end
      end

      it "should translate to netrc and cleanup" do
        # preconditions
        expect(File.exist?(@cli.legacy_credentials_path)).to eq(true)
        expect(File.exist?(@cli.netrc_path)).to eq(false)

        # transition
        expect(@cli.get_credentials.login).to eq('legacy_user')
        expect(@cli.get_credentials.password).to eq('legacy_pass')

        # postconditions
        expect(File.exist?(@cli.legacy_credentials_path)).to eq(false)
        expect(File.exist?(@cli.netrc_path)).to eq(true)
        expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].login).to eq('legacy_user')
        expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].password).to eq('legacy_pass')
      end
    end

    context "API key is set via environment variable" do
      before do
        ENV['HEROKU_API_KEY'] = "secret"
      end

      it "gets credentials from environment variables in preference to credentials file" do
        expect(@cli.read_credentials).to eq(['', ENV['HEROKU_API_KEY']])
      end

      it "returns a blank username" do
        expect(@cli.user).to be_empty
      end

      it "returns the api key as the password" do
        expect(@cli.password).to eq(ENV['HEROKU_API_KEY'])
      end

      it "does not overwrite credentials file with environment variable credentials" do
        expect(@cli).not_to receive(:write_credentials)
        @cli.read_credentials
      end

      context "reauthenticating" do
        before do
          allow(@cli).to receive(:ask_for_credentials).and_return(['new_user', 'new_password'])
          allow(@cli).to receive(:check)
          @cli.reauthorize
        end
        it "updates saved credentials" do
          expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].login).to eq('new_user')
          expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].password).to eq('new_password')
        end
        it "returns environment variable credentials" do
          expect(@cli.read_credentials).to eq(['', ENV['HEROKU_API_KEY']])
        end
      end

      context "logout" do
        before do
          @cli.logout
        end
        it "should delete saved credentials" do
          expect(File.exists?(@cli.legacy_credentials_path)).to be_falsey
          expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"]).to be_nil
        end
      end
    end

    describe "#base_host" do
      it "returns the host without the first part" do
        expect(@cli.base_host("http://foo.bar.com")).to eq("bar.com")
      end

      it "works with localhost" do
        expect(@cli.base_host("http://localhost:3000")).to eq("localhost")
      end
    end

    it "writes credentials and uploads authkey when credentials are saved" do
      allow(@cli).to receive(:credentials)
      allow(@cli).to receive(:check)
      allow(@cli).to receive(:ask_for_credentials).and_return(["username", "apikey"])
      expect(@cli).to receive(:write_credentials)
      @cli.ask_for_and_save_credentials
    end

    it "save_credentials deletes the credentials when the upload authkey is unauthorized" do
      allow(@cli).to receive(:write_credentials)
      allow(@cli).to receive(:retry_login?).and_return(false)
      allow(@cli).to receive(:ask_for_credentials).and_return(["username", "apikey"])
      allow(@cli).to receive(:check) { raise Heroku::API::Errors::Unauthorized.new("Login Failed", Excon::Response.new) }
      expect(@cli).to receive(:delete_credentials)
      expect { @cli.ask_for_and_save_credentials }.to raise_error(SystemExit)
    end

    it "asks for login again when not authorized, for three times" do
      allow(@cli).to receive(:read_credentials)
      allow(@cli).to receive(:write_credentials)
      allow(@cli).to receive(:delete_credentials)
      allow(@cli).to receive(:ask_for_credentials).and_return(["username", "apikey"])
      allow(@cli).to receive(:check) { raise Heroku::API::Errors::Unauthorized.new("Login Failed", Excon::Response.new) }
      expect(@cli).to receive(:ask_for_credentials).exactly(3).times
      expect { @cli.ask_for_and_save_credentials }.to raise_error(SystemExit)
    end

    it "deletes the credentials file" do
      FileUtils.mkdir_p(File.dirname(@cli.legacy_credentials_path))
      File.open(@cli.legacy_credentials_path, "w") do |file|
        file.puts "legacy_user\nlegacy_pass"
      end
      expect(FileUtils).to receive(:rm_f).with(@cli.legacy_credentials_path)
      @cli.delete_credentials
    end

    it "writes the login information to the credentials file for the 'heroku login' command" do
      allow(@cli).to receive(:ask_for_credentials).and_return(['one', 'two'])
      allow(@cli).to receive(:check)
      @cli.reauthorize
      expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].login).to eq('one')
      expect(Netrc.read(@cli.netrc_path)["api.#{@cli.host}"].password).to eq('two')
    end

    it "migrates long api keys to short api keys" do
      @cli.delete_credentials
      api_key = "7e262de8cac430d8a250793ce8d5b334ae56b4ff15767385121145198a2b4d2e195905ef8bf7cfc5"
      @cli.netrc["api.#{@cli.host}"] = ["user", api_key]

      expect(@cli.get_credentials.login).to eq("user")
      expect(@cli.get_credentials.password).to eq(api_key[0,40])
      Auth.subdomains.each do |section|
        expect(Netrc.read(@cli.netrc_path)["#{section}.#{@cli.host}"].login).to eq("user")
        expect(Netrc.read(@cli.netrc_path)["#{section}.#{@cli.host}"].password).to eq(api_key[0,40])
      end
    end
  end
end
