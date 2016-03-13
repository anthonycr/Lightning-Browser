require "spec_helper"
require "heroku/command"
require 'json' #FOR WEBMOCK

class FakeResponse

  attr_accessor :body, :headers

  def initialize(attributes)
    self.body, self.headers = attributes[:body], attributes[:headers]
  end

  def to_s
    body
  end

end

describe Heroku::Command do
  before {
    Heroku::Command.load
    stub_core # setup fake auth
  }

  describe "when the command requires confirmation" do
    include Support::Addons

    let(:response_that_requires_confirmation) do
      {:status => 423,
       :headers => { :x_confirmation_required => 'my_addon' },
       :body => 'terms of service required'}
    end

    before do
      Excon.stub(method: :post, path: %r(/apps/[^/]+/addons)) do |args|
        { body: MultiJson.encode(build_addon(name: "my_addon", app: { name: "example" })), status: 201 }
      end
    end

    after do
      Excon.stubs.shift
    end

    context "when the app is unknown" do
      context "and the user includes --confirm APP" do
        it "should set --app to APP and not ask for confirmation" do
          stub_request(:post, %r{apps/XXX/addons/my_addon$}).
            with(:body => {:confirm => "XXX"})
          run "addons:add my_addon --confirm XXX"
        end
      end

      context "and the user includes --confirm APP --app APP2" do
        it "should warn that the app and confirm do not match and not continue" do
          expect(capture_stderr do
            run "addons:add my_addon --confirm APP --app APP2"
          end).to eq(" !    Mismatch between --app and --confirm\n")
        end
      end
    end

    context "and the app is known" do
      before do
        any_instance_of(Heroku::Command::Base) do |base|
          stub(base).app.returns("example")
        end
      end

      context "and the user includes --confirm WRONGAPP" do
        it "should not allow include the option" do
          stub_request(:post, %r{apps/example/addons/my_addon$}).
            with(:body => "")
          run "addons:add my_addon --confirm XXX"
        end
      end

      context "and the user includes --confirm APP" do
        it "should set --app to APP and not ask for confirmation" do
          addon = build_addon(name: "my_addon", app: { name: "example" })

          Excon.stub(method: :post, path: %r(/apps/example/addons)) { |args|
            expect(args[:body]).to include '"confirm":"example"'
            { body: MultiJson.encode(build_addon(name: "my_addon", app: { name: "example" })), status: 201 }
          }

          run "addons:add my_addon --confirm example"

          Excon.stubs.shift
        end
      end

      context "and the user didn't include a confirm flag" do
        it "should ask the user for confirmation" do
          stub(Heroku::Command).confirm_command.returns(true)
          stub_request(:post, %r{apps/example/addons/my_addon$}).
            to_return(response_that_requires_confirmation).then.
            to_return({:status => 200})

          run "addons:add my_addon"
        end

        it "should not continue if the confirmation does not match" do
          allow(Heroku::Command).to receive(:current_options).and_return(:confirm => 'not_example')

          expect do
            Heroku::Command.confirm_command('example')
          end.to raise_error(Heroku::Command::CommandFailed)
        end

        it "should not continue if the user doesn't confirm" do
          stub(Heroku::Command).confirm_command.returns(false)
          stub_request(:post, %r{apps/example/addons/my_addon$}).
            to_return(response_that_requires_confirmation).then.
            to_raise(Heroku::Command::CommandFailed)

          run "addons:add my_addon"
        end
      end
    end
  end

  describe "parsing errors" do
    before do
      Excon.stub(method: :post, path: %r(/apps/example/addons)) { |args|
        { body: MultiJson.encode(build_addon(name: "my_addon", app: { name: "example" })), status: 201 }
      }
    end

    after do
      Excon.stubs.shift
    end

    it "extracts error messages from response when available in XML" do
      expect(Heroku::Command.extract_error('<errors><error>Invalid app name</error></errors>')).to eq('Invalid app name')
    end

    it "extracts error messages from response when available in JSON" do
      expect(Heroku::Command.extract_error("{\"error\":\"Invalid app name\"}")).to eq('Invalid app name')
    end

    it "extracts error messages from response when available in plain text" do
      response = FakeResponse.new(:body => "Invalid app name", :headers => { :content_type => "text/plain; charset=UTF8" })
      expect(Heroku::Command.extract_error(response)).to eq('Invalid app name')
    end

    it "shows Internal Server Error when the response doesn't contain a XML or JSON" do
      expect(Heroku::Command.extract_error('<h1>HTTP 500</h1>')).to eq("Internal server error.\nRun `heroku status` to check for known platform issues.")
    end

    it "shows Internal Server Error when the response is not plain text" do
      response = FakeResponse.new(:body => "Foobar", :headers => { :content_type => "application/xml" })
      expect(Heroku::Command.extract_error(response)).to eq("Internal server error.\nRun `heroku status` to check for known platform issues.")
    end

    it "allows a block to redefine the default error" do
      expect(Heroku::Command.extract_error("Foobar") { "Ok!" }).to eq('Ok!')
    end

    it "doesn't format the response if set to raw" do
      expect(Heroku::Command.extract_error("Foobar", :raw => true) { "Ok!" }).to eq('Ok!')
    end

    it "handles a nil body in parse_error_xml" do
      expect { Heroku::Command.parse_error_xml(nil) }.not_to raise_error
    end

    it "handles a nil body in parse_error_json" do
      expect { Heroku::Command.parse_error_json(nil) }.not_to raise_error
    end
  end

  it "correctly resolves commands" do
    class Heroku::Command::Test; end
    class Heroku::Command::Test::Multiple; end

    require "heroku/command/help"
    require "heroku/command/apps"

    expect(Heroku::Command.parse("unknown")).to be_nil
    expect(Heroku::Command.parse("list")).to include(:klass => Heroku::Command::Apps, :method => :index)
    expect(Heroku::Command.parse("apps")).to include(:klass => Heroku::Command::Apps, :method => :index)
    expect(Heroku::Command.parse("apps:create")).to include(:klass => Heroku::Command::Apps, :method => :create)
  end

  context "help" do
    it "works as a prefix" do
      expect(heroku("help ps:scale")).to match(/scale dynos by/)
    end

    it "works as an option" do
      expect(heroku("ps:scale -h")).to match(/scale dynos by/)
      expect(heroku("ps:scale --help")).to match(/scale dynos by/)
    end
  end

  context "when no commands match" do

    it "displays the version if --version is used" do
      expect(heroku("--version")).to eq <<-STDOUT
#{Heroku.user_agent}
heroku-cli/4.0.0-4f2c5c5 (amd64-darwin) go1.5
You have no installed plugins.
STDOUT
    end

    it "suggests similar commands if there are any" do
      original_stderr, original_stdout = $stderr, $stdout
      $stderr = captured_stderr = StringIO.new
      $stdout = captured_stdout = StringIO.new
      begin
        execute("aps")
      rescue SystemExit
      end
      expect(captured_stderr.string).to eq <<-STDERR
 !    `aps` is not a heroku command.
 !    Perhaps you meant `apps` or `ps`.
 !    See `heroku help` for a list of available commands.
STDERR
      expect(captured_stdout.string).to eq("")
      $stderr, $stdout = original_stderr, original_stdout
    end

    it "does not suggest similar commands if there are none" do
      original_stderr, original_stdout = $stderr, $stdout
      $stderr = captured_stderr = StringIO.new
      $stdout = captured_stdout = StringIO.new
      begin
        execute("sandwich")
      rescue SystemExit
      end
      expect(captured_stderr.string).to eq <<-STDERR
 !    `sandwich` is not a heroku command.
 !    See `heroku help` for a list of available commands.
STDERR
      expect(captured_stdout.string).to eq("")
      $stderr, $stdout = original_stderr, original_stdout
    end

  end
end
