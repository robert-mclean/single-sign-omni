import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import axios from "axios";
import { useState } from "react";
import {
  TextField,
  Card,
  Container,
  Typography,
  Button,
  Select,
  MenuItem,
} from "@mui/material";

const formSchema = z.object({
  assertionConsumerUrl: z.string().url("ACS be a valid URL"),
  spEntityId: z.string().min(3, "Service Provider Entity ID is required"),
  idpEntityId: z.string().min(3, "Service Provider Entity ID is required"),
  nameIdValue: z.string().min(3, "Name ID is required"),
  attributes: z.string().transform((attibutesText) =>
    attibutesText.split(";").map((attibuteText) => {
      const pair = attibuteText.split("=");
      return {
        name: pair[0],
        value: pair[1],
      };
    })
  ),
});

type FormData = z.infer<typeof formSchema>;

const HomePage = () => {
  const [loading, setLoading] = useState(false);
  const {
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(formSchema),
  });

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      data.attributes = data.attributes || "";
      const response = await axios.post(
        `${import.meta.env.VITE_API_URL}/generate`,
        data
      );

      console.log(response);

      if (response.data && response.data) {
        const form = document.createElement("form");
        form.method = "POST";
        form.action = data.assertionConsumerUrl;

        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "SAMLResponse";
        input.value = response.data;

        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
      } else {
        console.error("Invalid response from server");
      }
    } catch (error) {
      console.error("Error generating SAML response:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Container>
        <Typography variant="h2">Single Sign Omni</Typography>
      </Container>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Card variant="outlined">
          <Container>
            <Typography>Identity Provider Configuration</Typography>
            <Controller
              name="idpEntityId"
              control={control}
              defaultValue=""
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Identity Provider Entity ID"
                  variant="outlined"
                  fullWidth
                  margin="normal"
                />
              )}
            />
          </Container>
        </Card>
        <br />
        <Card variant="outlined">
          <Container>
            <Typography>Service Provider Configuration</Typography>
            <Controller
              name="assertionConsumerUrl"
              control={control}
              defaultValue=""
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Assertion Consumer Service"
                  variant="outlined"
                  fullWidth
                  margin="normal"
                />
              )}
            />

            <Controller
              name="spEntityId"
              control={control}
              defaultValue=""
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Service Provider Entity ID"
                  variant="outlined"
                  fullWidth
                  margin="normal"
                />
              )}
            />
          </Container>
        </Card>
        <br></br>
        <Card variant="outlined">
          <Container>
            <Typography>Assertion</Typography>
            <Controller
              name="nameIdValue"
              control={control}
              defaultValue=""
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Name ID"
                  variant="outlined"
                  fullWidth
                  margin="normal"
                />
              )}
            />
            <Controller
              name="attributes"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Attributes"
                  variant="outlined"
                  fullWidth
                  multiline
                  minRows="2"
                  margin="normal"
                />
              )}
            />
          </Container>
        </Card>
        <Container>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? "Submitting..." : "Generate SAML Response"}
          </Button>
        </Container>
      </form>
    </div>
  );
};

export default HomePage;
