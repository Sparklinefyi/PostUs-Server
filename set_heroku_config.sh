#!/bin/bash
# Read the .env file line by line
while IFS= read -r line
do
  # Skip empty lines and lines starting with #
  if [[ ! -z "$line" && ! "$line" =~ ^# ]]; then
    # Set the variable on Heroku
    heroku config:set "$line" -a sparkline
  fi
done < .env