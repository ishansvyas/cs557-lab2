#!/bin/bash

echo "export BEETHOVEN_PATH=$HOME/Beethoven" >> ~/.bashrc
echo "export PATH=$BEETHOVEN_PATH/bin:\$PATH" >> ~/.bashrc
source ~/.bashrc

sudo apt-get install cmake

cd ~
git clone https://github.com/Composer-Team/Beethoven
cd Beethoven

git clone https://github.com/aws/aws-fpga

# Install software lib
git clone https://github.com/Composer-Team/Beethoven-Software
mkdir -p Beethoven-Software/build
cd Beethoven-Software/build
cmake .. -DCMAKE_BUILD_TYPE=Release -DPLATFORM=discrete
make -j
sudo make install

# Clone runtime
cd $BEETHOVEN_PATH
git clone https://github.com/Composer-Team/Beethoven-Runtime