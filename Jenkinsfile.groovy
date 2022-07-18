pipeline {
    agent any
    stages {
        stage('infracost') {
            agent {
                any {
                    // Always use the latest 0.10.x version to pick up bug fixes and new resources.
                    // See https://www.infracost.io/docs/integrations/cicd/#docker-images for other options
                    image 'infracost/infracost:ci-0.10'
                    args "--user=root --entrypoint=''"
                }
            }

            // Set up any required credentials for posting the comment, e.g. GitHub token, GitLab token
            environment {
                INFRACOST_API_KEY = 'v7i8AkGKwMPZTZBb7BcKJ0BrVOuQWxBA'
                // If you're using Terraform Cloud/Enterprise and have variables or private modules stored
                // on there, specify the following to automatically retrieve the variables:
                // INFRACOST_TERRAFORM_CLOUD_TOKEN: credentials('jenkins-infracost-tfc-token')
                // Change this if you're using Terraform Enterprise
                // INFRACOST_TERRAFORM_CLOUD_HOST: app.terraform.io
            }

            steps {
                // Clone the base branch of the pull request (e.g. main/master) into a temp directory.
                sh 'ls -la'
                sh 'rm -rf ./*'
                sh 'rm -rf /tmp/base'
                sh 'git clone $GIT_URL --branch=master --single-branch /tmp/base'
                sh 'ls -la'
                sh 'pwd'
                sh 'infracost --version'

                // Generate Infracost JSON file as the baseline, add any required sub-directories to path, e.g. `/tmp/base/PATH/TO/TERRAFORM/CODE`.
                sh 'infracost breakdown --path=/tmp/base \
                                        --format=json \
                                        --out-file=/tmp/infracost-base.json'

                // Generate an Infracost diff and save it to a JSON file.
                sh 'infracost diff --path=/tmp/base/sample1 \
                                   --format=json \
                                   --compare-to=/tmp/infracost-base.json \
                                   --out-file=/tmp/infracost.json'

                // Posts a comment to the PR using the 'update' behavior.
                // This creates a single comment and updates it. The "quietest" option.
                // The other valid behaviors are:
                //   delete-and-new - Delete previous comments and create a new one.
                //   hide-and-new - Minimize previous comments and create a new one.
                //   new - Create a new cost estimate comment on every push.
                // See https://www.infracost.io/docs/features/cli_commands/#comment-on-pull-requests for other options.
                // The INFRACOST_ENABLE_CLOUD​=true section instructs the CLI to send its JSON output to Infracost Cloud.
                //   This SaaS product gives you visibility across all changes in a dashboard. The JSON output does not
                //   contain any cloud credentials or secrets.
                sh 'infracost comment github --path=/tmp/infracost.json --repo=priyankachouk/infracost-demo --pull-request=7 --github-token='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' --behavior=update'
            }
        }
    }
}
