require "zip"

namespace :zip do
  desc "build zip"
  task :build => dist("heroku-#{version}.zip")

  desc "sign zip"
  task :sign => dist("heroku-#{version}.zip.sha256")

  desc "release zip"
  task :release => [:build, :sign] do |t|
    s3_store dist("heroku-#{version}.zip"), "heroku-client/heroku-client-#{version}.zip"
    s3_store dist("heroku-#{version}.zip"), "heroku-client/heroku-client-beta.zip" if beta?
    s3_store dist("heroku-#{version}.zip"), "heroku-client/heroku-client.zip" unless beta?

    sh "heroku config:add UPDATE_HASH=#{zip_signature} -a toolbelt" unless beta?
  end

  file dist("heroku-#{version}.zip") => distribution_files("zip") do |t|
    tempdir do |dir|
      mkdir "heroku-client"
      cd "heroku-client" do
        assemble_distribution
        assemble_gems
        Zip::File.open(t.name, Zip::File::CREATE) do |zip|
          Dir["**/*"].each do |file|
            zip.add(file, file) { true }
          end
        end
      end
    end
  end

  file dist("heroku-#{version}.zip.sha256") => dist("heroku-#{version}.zip") do |t|
    File.open(t.name, "w") do |file|
      file.puts Digest::SHA256.file(t.prerequisites.first).hexdigest
    end
  end

  def zip_signature
    File.read(dist("heroku-#{version}.zip.sha256")).chomp
  end
end
