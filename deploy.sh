./gradlew clean build || { echo 'Build failed' ; return 1; }
pushd build/site
find . -type f -exec curl --user $TEAVM_DEPLOY_LOGIN:$TEAVM_DEPLOY_PASSWORD \
  --ftp-create-dirs \
  -T {} sftp://$TEAVM_DEPLOY_SERVER/{} \
  \;
popd