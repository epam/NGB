# Copyright 2023 EPAM Systems, Inc. (https://www.epam.com/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM accetto/ubuntu-vnc-xfce-g3:20.04

# Switch to root user to install additional software
USER 0

# Install common
RUN apt-get update && \
    apt-get install -y wget \
                   openjdk-8-jre

RUN apt-get install fonts-liberation libcurl3-gnutls libcurl3-nss libcurl4 libgbm1 libu2f-udev -y

RUN wget -N https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_112.0.5615.165-1_amd64.deb -P ~/ && \
    dpkg -i google-chrome-stable_112.0.5615.165-1_amd64.deb && \
    rm -f google-chrome-stable_112.0.5615.165-1_amd64.deb
RUN apt-get install unzip -y

RUN wget -N https://chromedriver.storage.googleapis.com/112.0.5615.49/chromedriver_linux64.zip -P ~/ && \
    unzip chromedriver_linux64.zip && \
    mv -f ~/chromedriver /usr/local/bin/chromedriver && \
    chown root:root /usr/local/bin/chromedriver && \
    chmod 0755 /usr/local/bin/chromedriver

# Test prerequisites
ENV USER_HOME_DIR="/headless"
RUN mkdir -p /$USER_HOME_DIR/Downloads
WORKDIR /$USER_HOME_DIR/e2e/gui

# Run tests
ADD run.sh /tmp/run.sh
RUN chmod +x /tmp/run.sh

# switch back to default user
USER 1000

CMD bash /tmp/run.sh
