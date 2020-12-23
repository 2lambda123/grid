FROM mozilla/sbt

RUN apt-get -y update

RUN apt-get -y install graphicsmagick
RUN apt-get -y install graphicsmagick-imagemagick-compat
RUN apt-get -y install exiftool
RUN apt-get -y install pngquant

ENTRYPOINT ["sbt"]
