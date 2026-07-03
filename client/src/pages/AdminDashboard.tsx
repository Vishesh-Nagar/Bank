import React, { useState, useEffect } from "react";
import {
	Container,
	Box,
	Typography,
	Paper,
	Tabs,
	Tab,
	Button,
	CircularProgress,
	Alert,
	Table,
	TableBody,
	TableCell,
	TableContainer,
	TableHead,
	TableRow,
} from "@mui/material";
import api from "../services/api";

const AdminDashboard: React.FC = () => {
	const [tab, setTab] = useState(0);

	return (
		<Container maxWidth="xl">
			<Box sx={{ paddingY: 4 }}>
				<Typography
					variant="h4"
					sx={{ color: "#ffffff", mb: 3, fontWeight: "bold" }}
				>
					Admin Dashboard
				</Typography>

				<Paper
					sx={{
						backgroundColor: "#1e1e1e",
						padding: 3,
						borderRadius: 2,
					}}
				>
					<Tabs
						value={tab}
						onChange={(_e, val) => setTab(val)}
						textColor="inherit"
						sx={{
							"& .MuiTabs-indicator": {
								backgroundColor: "#1976d2",
							},
							"& .MuiTab-root": {
								color: "#aaaaaa",
								"&.Mui-selected": { color: "#ffffff" },
							},
						}}
					>
						<Tab label="Users" />
						<Tab label="System Logs" />
					</Tabs>

					<Box sx={{ mt: 3 }}>
						{tab === 0 && <UsersTab />}
						{tab === 1 && <LogsTab />}
					</Box>
				</Paper>
			</Box>
		</Container>
	);
};

const UsersTab: React.FC = () => {
	const [users, setUsers] = useState<any[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const fetchUsers = async () => {
		try {
			setLoading(true);
			const res = await api.get("/api/v1/users");
			setUsers(res.data.data.content || []);
		} catch (err: any) {
			setError("Failed to load users");
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchUsers();
	}, []);

	const toggleLock = async (id: number, currentStatus: boolean) => {
		try {
			await api.put(`/api/v1/users/${id}/lock`, null, {
				params: { locked: !currentStatus },
			});
			fetchUsers();
		} catch (err) {
			alert("Failed to update user lock status");
		}
	};

	if (loading) return <CircularProgress />;
	if (error) return <Alert severity="error">{error}</Alert>;

	return (
		<TableContainer component={Paper} sx={{ backgroundColor: "#2e2e2e" }}>
			<Table>
				<TableHead>
					<TableRow>
						<TableCell sx={{ color: "#aaaaaa" }}>ID</TableCell>
						<TableCell sx={{ color: "#aaaaaa" }}>
							Username
						</TableCell>
						<TableCell sx={{ color: "#aaaaaa" }}>Email</TableCell>
						<TableCell sx={{ color: "#aaaaaa" }}>Role</TableCell>
						<TableCell sx={{ color: "#aaaaaa" }}>Locked?</TableCell>
						<TableCell sx={{ color: "#aaaaaa" }}>Action</TableCell>
					</TableRow>
				</TableHead>
				<TableBody>
					{users.map((u) => (
						<TableRow key={u.id}>
							<TableCell sx={{ color: "#ffffff" }}>
								{u.id}
							</TableCell>
							<TableCell sx={{ color: "#ffffff" }}>
								{u.username}
							</TableCell>
							<TableCell sx={{ color: "#ffffff" }}>
								{u.email}
							</TableCell>
							<TableCell sx={{ color: "#ffffff" }}>
								{u.role}
							</TableCell>
							<TableCell sx={{ color: "#ffffff" }}>
								{u.accountLocked ? "Yes" : "No"}
							</TableCell>
							<TableCell>
								<Button
									variant="outlined"
									color={
										u.accountLocked ? "success" : "error"
									}
									onClick={() =>
										toggleLock(u.id, u.accountLocked)
									}
								>
									{u.accountLocked ? "Unlock" : "Lock"}
								</Button>
							</TableCell>
						</TableRow>
					))}
				</TableBody>
			</Table>
		</TableContainer>
	);
};

const LogsTab: React.FC = () => {
	const [serviceName, setServiceName] = useState("user-service");
	const [logs, setLogs] = useState<string[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const fetchLogs = async () => {
		try {
			setLoading(true);
			setError(null);
			const res = await api.get("/api/v1/admin/logs", {
				params: { serviceName, lines: 200 },
			});
			setLogs(res.data.data || []);
		} catch (err: any) {
			setError(err.response?.data?.message || "Failed to load logs");
			setLogs([]);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchLogs();
	}, [serviceName]);

	return (
		<Box>
			<Box sx={{ mb: 2 }}>
				{[
					"user-service",
					"account-service",
					"payment-service",
					"notification-service",
				].map((s) => (
					<Button
						key={s}
						variant={serviceName === s ? "contained" : "outlined"}
						onClick={() => setServiceName(s)}
						sx={{ mr: 1, textTransform: "none" }}
					>
						{s}
					</Button>
				))}
				<Button onClick={fetchLogs} sx={{ ml: 2, color: "#aaaaaa" }}>
					Refresh
				</Button>
			</Box>

			{loading && <CircularProgress size={24} sx={{ mb: 2 }} />}
			{error && (
				<Alert severity="error" sx={{ mb: 2 }}>
					{error}
				</Alert>
			)}

			<Paper
				sx={{
					backgroundColor: "#000000",
					p: 2,
					maxHeight: "60vh",
					overflow: "auto",
					fontFamily: "monospace",
					color: "#00ff00",
				}}
			>
				{logs.length === 0 && !loading && (
					<Typography>No logs available</Typography>
				)}
				{logs.map((l, i) => (
					<div
						key={i}
						style={{
							whiteSpace: "pre-wrap",
							wordBreak: "break-all",
						}}
					>
						{l}
					</div>
				))}
			</Paper>
		</Box>
	);
};

export default AdminDashboard;
