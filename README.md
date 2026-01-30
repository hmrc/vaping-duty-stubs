
# Vaping Duty Stubs

# Returning specific stubbed information

Characters in vpdId are used to return specific stubbed information: (rightmost character in the string is character 1)

Character 9 - contact preferences submission

Character 10 - subscription data (existing contact preferences)

Characters in credId are used when calling the email-verification service APIs:

Character 1 - email verification statuses (only if email-verification-stub is toggled on in vaping-duty-account)

See the relevant controllers for more detail on the responses

## Running this stub
### Run the stub using sm2
To run the stub using sm2, use the following command:

```sh
sm2 --start VAPING_DUTY_STUBS
```

### Run the stub locally
To run the stub locally without using sm2, first:

- clone the repository
- cd into the cloned repo in your shell
- run the following command:

```sh
sbt run
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
