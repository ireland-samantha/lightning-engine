/*
 * Copyright (c) 2026 Samantha Ireland
 * MIT License
 */

import { Close as CloseIcon } from "@mui/icons-material";
import { Alert, AlertTitle, IconButton, Slide, Snackbar } from "@mui/material";
import type { SlideProps } from "@mui/material/Slide";
import { useCallback, useEffect, useState } from "react";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import {
  removeNotification,
  selectLatestNotification,
  type Notification,
} from "../store/slices/notificationSlice";

function SlideTransition(props: SlideProps) {
  return <Slide {...props} direction="left" />;
}

const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const dispatch = useAppDispatch();
  const latestNotification = useAppSelector(selectLatestNotification);
  const [currentNotification, setCurrentNotification] =
    useState<Notification | null>(null);
  const [open, setOpen] = useState(false);

  // Show new notifications
  useEffect(() => {
    if (
      latestNotification &&
      latestNotification.id !== currentNotification?.id
    ) {
      setCurrentNotification(latestNotification);
      setOpen(true);
    }
  }, [latestNotification, currentNotification?.id]);

  const handleClose = useCallback(
    (_event?: React.SyntheticEvent | Event, reason?: string) => {
      if (reason === "clickaway") {
        return;
      }
      setOpen(false);
    },
    []
  );

  const handleExited = useCallback(() => {
    if (currentNotification) {
      dispatch(removeNotification(currentNotification.id));
      setCurrentNotification(null);
    }
  }, [currentNotification, dispatch]);

  return (
    <>
      {children}
      <Snackbar
        open={open}
        autoHideDuration={
          currentNotification?.severity === "error" ? 8000 : 5000
        }
        onClose={handleClose}
        TransitionComponent={SlideTransition}
        TransitionProps={{ onExited: handleExited }}
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
        sx={{ mt: 8 }}
      >
        <Alert
          severity={currentNotification?.severity || "info"}
          variant="filled"
          onClose={handleClose}
          action={
            <IconButton
              size="small"
              color="inherit"
              onClick={handleClose}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          }
          sx={{
            minWidth: 300,
            maxWidth: 500,
            boxShadow: 6,
          }}
        >
          {currentNotification?.source && (
            <AlertTitle sx={{ fontFamily: "monospace", fontSize: "0.85rem" }}>
              {currentNotification.source}
            </AlertTitle>
          )}
          {currentNotification?.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default NotificationProvider;
