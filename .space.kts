/**
* JetBrains Space Automation
* This Kotlin-script file lets you automate build activities
* For more info, refer to https://www.jetbrains.com/help/space/automation.html
*/

job("Build and deploy backend for web") {
    container("gradle:jdk8") {
        env["IP"] = Params("web_backend_ip")
        env["DIR"] = Params("web_backend_dir")
        env["STARTKEY"] = Secrets("web_backend_key_1")
        env["ENDKEY"] = Secrets("web_backend_key_2")
        shellScript {
            content = """
            	gradle war
                touch key.pem
                echo ${"$"}STARTKEY ${"$"}ENDKEY | sed 's/ /\n/g;w key.pem'  > /dev/null 2>&1
                sed -i '1s/^/-----BEGIN OPENSSH PRIVATE KEY-----\n/' key.pem
                echo "\n-----END OPENSSH PRIVATE KEY-----" >> key.pem
                chmod 600 key.pem
                scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i key.pem -r build/libs/* root@${"$"}IP:${"$"}DIR
            """
        }
    }
}